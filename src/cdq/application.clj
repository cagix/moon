(ns cdq.application
  (:require cdq.utils
            clojure.gdx.application
            clojure.gdx.backends.lwjgl
            [cdq.gdx.utils.viewport :as viewport]
            clojure.java.io))

(def create-components
  '[[:cdq/entity-states                 cdq.create.entity.state]
    [:cdq/effects                       cdq.create.effects]
    [:cdq/schemas                       cdq.create.schemas]
    [:cdq/db                            cdq.create.db]
    [:cdq/assets                        cdq.create.assets]
    [:cdq.graphics/batch                cdq.create.batch]
    [:cdq.graphics/shape-drawer-texture cdq.create.shape-drawer-texture]
    [:cdq.graphics/shape-drawer         cdq.create.shape-drawer]
    [:cdq.graphics/cursors              cdq.create.cursors]
    [:cdq.graphics/default-font         cdq.create.default-font]
    [:cdq.graphics/world-unit-scale     cdq.create.world-unit-scale]
    [:cdq.graphics/tiled-map-renderer   cdq.create.tiled-map-renderer]
    [:cdq.graphics/ui-viewport          cdq.create.ui-viewport]
    [:cdq.graphics/world-viewport       cdq.create.world-viewport]
    [:cdq.context/stage                 cdq.create.stage]
    [:cdq.context/elapsed-time          cdq.create.elapsed-time]
    [:cdq.context/player-message        cdq.create.player-message]
    [:cdq.context/level                 cdq.create.level]
    [:cdq.context/error                 cdq.create.error]
    [:cdq.context/tiled-map             cdq.create.tiled-map]
    [:cdq.context/explored-tile-corners cdq.create.explored-tile-corners]
    [:cdq.context/grid                  cdq.create.grid]
    [:cdq.context/raycaster             cdq.create.raycaster]
    [:cdq.context/content-grid          cdq.create.content-grid]
    [:cdq.context/entity-ids            cdq.create.entity-ids]
    [:cdq.context/factions-iterations   cdq.create.factions-iterations]
    [:world/potential-field-cache       cdq.create.potential-fields]
    [:cdq.context/player-eid            cdq.create.player-eid]])

(def render-fns
  '[(cdq.render.assoc-active-entities/render)
    (cdq.render.camera/set-on-player)
    (cdq.gdx.graphics/clear-screen)
    (cdq.render.tiled-map/draw)
    (cdq.graphics/draw-on-world-view [(cdq.render.before-entities/render)
                                      (cdq.world.graphics/render-entities
                                       {:below {:entity/mouseover? cdq.world.graphics/draw-faction-ellipse
                                                :player-item-on-cursor cdq.world.graphics/draw-world-item-if-exists
                                                :stunned cdq.world.graphics/draw-stunned-circle}
                                        :default {:entity/image cdq.world.graphics/draw-image-as-of-body
                                                  :entity/clickable cdq.world.graphics/draw-text-when-mouseover-and-text
                                                  :entity/line-render cdq.world.graphics/draw-line}
                                        :above {:npc-sleeping cdq.world.graphics/draw-zzzz
                                                :entity/string-effect cdq.world.graphics/draw-text
                                                :entity/temp-modifier cdq.world.graphics/draw-filled-circle-grey}
                                        :info {:entity/hp cdq.world.graphics/draw-hpbar-when-mouseover-and-not-full
                                               :active-skill cdq.world.graphics/draw-skill-image-and-active-effect}})
                                      (cdq.render.after-entities/render)])
    (cdq.render/draw-stage)
    (cdq.render/update-stage)
    (cdq.render/player-state-input)
    (cdq.render/update-mouseover-entity)
    (cdq.render/update-paused)
    (cdq.render/when-not-paused)
    (cdq.render/remove-destroyed-entities)
    (cdq.render/camera-controls)
    (cdq.render/window-controls)])

(def state (atom nil))

; oh ok ! or configure a libgdx application on the fly with clojure.core/fetch-deps or fetch-libs
; and restart it
; and see the context @ state
; or errors
; fps etc

(defn -main []
  (.setIconImage (java.awt.Taskbar/getTaskbar)
                 (.getImage (java.awt.Toolkit/getDefaultToolkit)
                            (clojure.java.io/resource "moon.png")))
  (when (= com.badlogic.gdx.utils.SharedLibraryLoader/os
           com.badlogic.gdx.utils.Os/MacOsX)
    (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (clojure.gdx.backends.lwjgl/application (reify clojure.gdx.application/Listener
                                            (create [_]
                                              (reset! state (reduce (fn [context [k ns-sym]]
                                                                      (require ns-sym)
                                                                      (let [f (resolve (symbol (str ns-sym "/create")))]
                                                                        (assoc context k (f context))))
                                                                    {}
                                                                    create-components)))

                                            (dispose [_]
                                              (doseq [[k value] @state
                                                      :when (cdq.utils/disposable? value)]
                                                ;(println "Disposing " k " - " value)
                                                (cdq.utils/dispose value)))

                                            (pause [_])

                                            (render [_]
                                              (swap! state (fn [context]
                                                             (reduce (fn [context fn-invoc]
                                                                       (cdq.utils/req-resolve-call fn-invoc context))
                                                                     context
                                                                     render-fns))))

                                            (resize [_ width height]
                                              (let [context @state]
                                                (viewport/update (:cdq.graphics/ui-viewport    context) width height :center-camera? true)
                                                (viewport/update (:cdq.graphics/world-viewport context) width height)))

                                            (resume [_]))
                                          {:title "Cyber Dungeon Quest"
                                           :windowed-mode {:width 1440
                                                           :height 900}
                                           :foreground-fps 60
                                           :opengl-emulation {:gl-version :gl20
                                                              :gles-3-major-version 3
                                                              :gles-3-minor-version 2}}))
