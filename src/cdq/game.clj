(ns cdq.game
  (:require cdq.application
            [cdq.create.assets :as assets]
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
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx.graphics.g2d SpriteBatch)
           (com.badlogic.gdx.utils SharedLibraryLoader Os)
           (com.badlogic.gdx.utils.viewport Viewport)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

; TODO * the application context without any game specific logic ! *
; just the mechanics !!
; clojure.rpg ?
; but where exactly does game specific logic start? at schemas ???
; !!! what a boundary !!!
; can also set background color ScreenUtils/clear ...
; fuck !

(defn- create-game []
  (let [schemas (schemas/create)
        batch (SpriteBatch.)
        shape-drawer-texture (shape-drawer-texture/create)
        world-unit-scale (world-unit-scale/create)
        ui-viewport (ui-viewport/create)
        context {:cdq/assets (assets/create {:folder "resources/"
                                             :asset-type->extensions {:sound   #{"wav"}
                                                                      :texture #{"png" "bmp"}}})
                 :cdq.graphics/batch batch
                 :cdq.graphics/cursors (cursors/create)
                 :cdq.graphics/default-font (default-font/create {:file "fonts/exocet/films.EXL_____.ttf"
                                                                  :size 16
                                                                  :quality-scaling 2})
                 :cdq.graphics/shape-drawer (shape-drawer/create batch shape-drawer-texture)
                 :cdq.graphics/shape-drawer-texture shape-drawer-texture
                 :cdq.graphics/tiled-map-renderer (tiled-map-renderer/create batch world-unit-scale)
                 :cdq.graphics/ui-viewport ui-viewport
                 :cdq.graphics/world-unit-scale world-unit-scale
                 :cdq.graphics/world-viewport (world-viewport/create world-unit-scale)
                 :cdq/db (db/create schemas)
                 :context/entity-components (cdq.create.entity-components/create)
                 :cdq/schemas schemas
                 :cdq.context/stage (stage/create batch ui-viewport)}]
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
  (Viewport/.update (:cdq.graphics/ui-viewport    context) width height true)
  (Viewport/.update (:cdq.graphics/world-viewport context) width height false))

(defn -main []
  (when  (= SharedLibraryLoader/os Os/MacOsX)
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource "moon.png")))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (clojure.gdx.backends.lwjgl/application (reify clojure.gdx.application/Listener
                                            (create [_]
                                              (reset! cdq.application/state (create-game)))

                                            (dispose [_]
                                              (dispose-game @cdq.application/state))

                                            (pause [_])

                                            (render [_]
                                              (swap! cdq.application/state render-game))

                                            (resize [_ width height]
                                              (resize-game @cdq.application/state width height))

                                            (resume [_]))
                                          {:title "Cyber Dungeon Quest"
                                           :windowed-mode {:width 1440
                                                           :height 900}
                                           :foreground-fps 60}))
