(ns cdq.application
  (:require [cdq.audio :as audio]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.ui :as ui]
            cdq.db.impl
            cdq.graphics.impl
            cdq.ui.impl
            cdq.game.create.txs
            cdq.game.render.get-stage-ctx
            cdq.game.render.validate
            cdq.game.render.update-mouse
            cdq.game.render.update-mouseover-eid
            cdq.game.render.check-open-debug
            cdq.game.render.assoc-active-entities
            cdq.game.render.set-camera-on-player
            cdq.game.render.clear-screen
            cdq.game.render.draw-world-map
            cdq.game.render.draw-on-world-viewport
            cdq.game.render.assoc-interaction-state
            cdq.game.render.set-cursor
            cdq.game.render.player-state-handle-input
            cdq.game.render.dissoc-interaction-state
            cdq.game.render.assoc-paused
            cdq.game.render.update-world-time
            cdq.game.render.update-potential-fields
            cdq.game.render.tick-entities
            cdq.game.render.remove-destroyed-entities
            cdq.game.render.window-camera-controls
            cdq.game.render.render-stage
            cdq.game.render.validate
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

(defn- render! [ctx]
  (-> ctx
      cdq.game.render.get-stage-ctx/step
      cdq.game.render.validate/step
      cdq.game.render.update-mouse/step
      cdq.game.render.update-mouseover-eid/step
      cdq.game.render.check-open-debug/step
      cdq.game.render.assoc-active-entities/step
      cdq.game.render.set-camera-on-player/step
      cdq.game.render.clear-screen/step
      cdq.game.render.draw-world-map/step
      cdq.game.render.draw-on-world-viewport/step
      cdq.game.render.assoc-interaction-state/step
      cdq.game.render.set-cursor/step
      cdq.game.render.player-state-handle-input/step
      cdq.game.render.dissoc-interaction-state/step
      cdq.game.render.assoc-paused/step
      cdq.game.render.update-world-time/step
      cdq.game.render.update-potential-fields/step
      cdq.game.render.tick-entities/step
      cdq.game.render.remove-destroyed-entities/step
      cdq.game.render.window-camera-controls/step
      cdq.game.render.render-stage/step
      cdq.game.render.validate/step))

(defn- resize! [{:keys [ctx/graphics]} width height]
  (graphics/update-ui-viewport! graphics width height)
  (graphics/update-world-vp! graphics width height))

(defn edn-resource [path]
  (->> path
       io/resource
       slurp
       (edn/read-string {:readers {'edn/resource edn-resource}})))

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
                            (swap! state render!))

                          (resize [_ width height]
                            (resize! @state width height))

                          (pause [_])

                          (resume [_]))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:width (:window config))
                                            (:height (:window config)))
                          (.setForegroundFPS (:fps config))))))
