(ns cdq.application
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.ui :as ui]
            cdq.ui.build.editor-window
            cdq.ui.editor.window
            cdq.ui.dev-menu
            cdq.ui.editor.overview-window
            [cdq.world :as world]
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

(defn- dispose!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/stage
           ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (ui/dispose! stage)
  (world/dispose! world))

(defn- resize! [{:keys [ctx/graphics]} width height]
  (graphics/update-ui-viewport! graphics width height)
  (graphics/update-world-vp! graphics width height))

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

(def state (atom nil))

(defn -main []
  (let [config (edn-resource "config.edn")]
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
                                                   (:render! config)))))

                          (resize [_ width height]
                            (resize! @state width height))

                          (pause [_])

                          (resume [_]))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:width (:window config))
                                            (:height (:window config)))
                          (.setForegroundFPS (:fps config))))))
