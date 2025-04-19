(ns cdq.game
  (:require [cdq.create.assets :as assets]
            [cdq.create.batch :as batch]
            [cdq.create.cursors :as cursors]
            [cdq.create.default-font :as default-font]
            [cdq.create.db :as db]
            cdq.create.effects
            cdq.create.entity-components
            [cdq.create.schemas :as schemas]
            [cdq.create.shape-drawer :as shape-drawer]
            [cdq.create.shape-drawer-texture :as shape-drawer-texture]
            [cdq.create.stage :as stage]
            [cdq.create.tiled-map-renderer :as tiled-map-renderer]
            [cdq.create.ui-viewport :as ui-viewport]
            [cdq.create.world-unit-scale :as world-unit-scale]
            [cdq.create.world-viewport :as world-viewport]
            cdq.world.context
            clojure.gdx.application
            clojure.gdx.backends.lwjgl
            [clojure.gdx.utils :as utils]
            [clojure.gdx.utils.viewport :as viewport]))

(defn- create-game []
  (let [schemas (schemas/create)
        batch (batch/create)
        shape-drawer-texture (shape-drawer-texture/create)
        world-unit-scale (world-unit-scale/create)
        ui-viewport (ui-viewport/create)
        context {:cdq/assets (assets/create)
                 :cdq.graphics/batch batch
                 :cdq.graphics/cursors (cursors/create)
                 :cdq.graphics/default-font (default-font/create)
                 :cdq.graphics/shape-drawer (shape-drawer/create batch shape-drawer-texture)
                 :cdq.graphics/shape-drawer-texture shape-drawer-texture
                 :cdq.graphics/tiled-map-renderer (tiled-map-renderer/create batch world-unit-scale)
                 :cdq.graphics/ui-viewport ui-viewport
                 :cdq.graphics/world-unit-scale world-unit-scale
                 :cdq.graphics/world-viewport (world-viewport/create world-unit-scale)
                 :cdq/db (db/create schemas)
                 :context/entity-components (cdq.create.entity-components/create)
                 :cdq/schemas schemas
                 :cdq.context/stage (stage/create batch ui-viewport)} ]
    (cdq.world.context/reset context :worlds/vampire)))

(defn- dispose-game [context]
  (doseq [[_k value] context
          :when (utils/disposable? value)]
    (utils/dispose value)))

(defn- render-game [context]
  (reduce (fn [context f]
            (f context))
          context
          (for [ns-sym '[cdq.render.assoc-active-entities
                         cdq.render.set-camera-on-player
                         cdq.render.clear-screen
                         cdq.render.tiled-map
                         cdq.render.draw-on-world-view
                         cdq.render.stage
                         cdq.render.player-state-input
                         cdq.render.update-mouseover-entity
                         cdq.render.update-paused
                         cdq.render.when-not-paused

                         ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                         cdq.render.remove-destroyed-entities

                         cdq.render.camera-controls
                         cdq.render.window-controls]]
            (do
             (require ns-sym)
             (resolve (symbol (str ns-sym "/render")))))))

(defn- resize-game [context width height]
  (viewport/update (:cdq.graphics/ui-viewport    context) width height :center-camera? true)
  (viewport/update (:cdq.graphics/world-viewport context) width height))

(def state (atom nil))

(defn -main []
  (clojure.utils/execute! (get {:mac '[(clojure.java.awt.taskbar/set-icon "moon.png")
                                       (clojure.lwjgl.system.configuration/set-glfw-library-name "glfw_async")]}
                               (clojure.gdx.utils/operating-system)))
  (clojure.gdx.backends.lwjgl/application (reify clojure.gdx.application/Listener
                                            (create [_]
                                              (reset! state (create-game)))

                                            (dispose [_]
                                              (dispose-game @state))

                                            (pause [_])

                                            (render [_]
                                              (swap! state render-game))

                                            (resize [_ width height]
                                              (resize-game @state width height))

                                            (resume [_]))
                                          {:title "Cyber Dungeon Quest"
                                           :windowed-mode {:width 1440
                                                           :height 900}
                                           :foreground-fps 60}))
