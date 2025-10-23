(ns cdq.application
  (:require cdq.ui.build.editor-window
            cdq.ui.editor.window
            cdq.ui.dev-menu
            cdq.ui.editor.overview-window
            [cdq.input :as input]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx Application
                             ApplicationListener
                             Gdx
                             Input)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration))
  (:gen-class))

(q/defrecord Context [])

(defn create!
  [^Application app config]
  (let [audio    (.getAudio    app)
        files    (.getFiles    app)
        graphics (.getGraphics app)
        input    (.getInput    app)
        graphics ((:graphics-impl config) graphics files (:graphics config))
        stage ((:ui-impl config) graphics (:ui-config config))
        ctx (-> (map->Context {})
                (assoc :ctx/graphics graphics)
                (assoc :ctx/stage stage)
                ((:handle-txs config))
                (assoc :ctx/audio ((:audio-impl config) audio files (:audio config)))
                (assoc :ctx/db ((:db-impl config)))
                (assoc :ctx/input input)
                (assoc :ctx/config {:world-impl (:world-impl config)}))]
    (.setInputProcessor input stage)
    ((:add-actors config) stage ctx)
    ((:create-world config) ctx (:world config))))

(def state (atom nil))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})
       (walk/postwalk (fn [form]
                        (if (and (symbol? form)
                                 (namespace form))
                          (let [avar (requiring-resolve form)]
                            (assert avar form)
                            avar)
                          form)))))

(defn -main []
  (let [{:keys [config
                dispose!
                render!
                resize!
                title
                window
                fps]} (edn-resource "config.edn")]
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (Lwjgl3Application. (reify ApplicationListener
                          (create [_]
                            (reset! state (create! Gdx/app config)))

                          (dispose [_]
                            (dispose! @state))

                          (render [_]
                            (swap! state (fn [ctx]
                                           (reduce (fn [ctx f]
                                                     (f ctx))
                                                   ctx
                                                   render!))))

                          (resize [_ width height]
                            (resize! @state width height))

                          (pause [_])

                          (resume [_]))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle title)
                          (.setWindowedMode (:width window)
                                            (:height window))
                          (.setForegroundFPS fps)))))
