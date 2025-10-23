(ns cdq.application
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.ui :as ui]
            cdq.db.impl
            cdq.graphics.impl
            cdq.ui.impl
            cdq.game.create.txs
            cdq.world.impl
            cdq.game.create.add-actors
            cdq.game.create.world
            cdq.game.create.dev-menu-config
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

(defn- create!
  [^Application app config]
  (let [audio    (.getAudio    app)
        files    (.getFiles    app)
        graphics (.getGraphics app)
        input    (.getInput    app)
        graphics (cdq.graphics.impl/create! graphics files (:graphics config))
        stage (cdq.ui.impl/create! graphics {:dev-menu cdq.game.create.dev-menu-config/create})
        ctx (-> (map->Context {})
                (assoc :ctx/graphics graphics)
                (assoc :ctx/stage stage)
                cdq.game.create.txs/do!
                (assoc :ctx/audio (cdq.audio/create audio files (:audio config)))
                (assoc :ctx/db (cdq.db.impl/create))
                (assoc :ctx/input input)
                (assoc :ctx/config {:world-impl cdq.world.impl/create}))]
    (.setInputProcessor input stage)
    (cdq.game.create.add-actors/step stage ctx)
    (cdq.game.create.world/step ctx (:world config))))

(defn- dispose!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/stage
           ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (ui/dispose! stage)
  (world/dispose! world))

(defn- render! [ctx steps]
  (reduce (fn [ctx f]
            (f ctx))
          ctx
          steps))

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
                            (swap! state render! (:render! config)))

                          (resize [_ width height]
                            (resize! @state width height))

                          (pause [_])

                          (resume [_]))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:width (:window config))
                                            (:height (:window config)))
                          (.setForegroundFPS (:fps config))))))
