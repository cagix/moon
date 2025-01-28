(ns cdq.application
  (:require [cdq.application.context :as context]
            [clojure.gdx.application :as application]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils :as utils]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.utils]))

(def state (atom nil))

(def context-keyset
  #{:cdq/assets
    :cdq/db
    :cdq/effects
    :cdq/schemas
    :cdq.context/content-grid

    ; delta-time gets added after first frame
    :cdq.context/delta-time

    :cdq.context/elapsed-time
    :cdq.context/entity-ids
    :cdq.context/error
    :cdq.context/explored-tile-corners
    :cdq.context/factions-iterations
    :cdq.context/grid
    :cdq.context/level
    :cdq.context/mouseover-eid
    :cdq.context/paused?
    :cdq.context/player-eid
    :cdq.context/player-message
    :cdq.context/raycaster
    :cdq.context/stage
    :cdq.context/tiled-map
    :cdq.game/active-entities
    :cdq.graphics/batch
    :cdq.graphics/cursors
    :cdq.graphics/default-font
    :cdq.graphics/shape-drawer
    :cdq.graphics/shape-drawer-texture
    :cdq.graphics/tiled-map-renderer
    :cdq.graphics/ui-viewport
    :cdq.graphics/world-unit-scale
    :cdq.graphics/world-viewport
    :context/entity-components
    :world/potential-field-cache}
  )

(comment
 (clojure.pprint/pprint (sort (keys @state)))

 (= context-keyset (set (keys @state)))
 ; => validate each frame !
 ; => after each render/effect ?
 )

(defn -main []
  (clojure.utils/execute! (get {:mac '[(clojure.java.awt.taskbar/set-icon "moon.png")
                                       (clojure.lwjgl.system.configuration/set-glfw-library-name "glfw_async")]}
                               (clojure.gdx.utils/operating-system)))
  (lwjgl/application (reify application/Listener
                       (create [_]
                         (reset! state (context/create)))

                       (dispose [_]
                         (comment
                          (context/dispose @state)
                          )

                         (doseq [[k value] @state
                                 :when (utils/disposable? value)]
                           (utils/dispose value)))

                       (pause [_])

                       (render [_]
                         (comment
                          (swap! state context/render)
                          )

                         (swap! state (fn [context]
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
                                                   (resolve (symbol (str ns-sym "/render")))))))))

                       (resize [_ width height]
                         (comment
                          (context/resize @state width height)
                          )

                         (let [context @state]
                           (viewport/update (:cdq.graphics/ui-viewport    context) width height :center-camera? true)
                           (viewport/update (:cdq.graphics/world-viewport context) width height)))

                       (resume [_]))
                     {:title "Cyber Dungeon Quest"
                      :windowed-mode {:width 1440
                                      :height 900}
                      :foreground-fps 60}))
