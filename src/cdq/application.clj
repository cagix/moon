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
    (cdq.render.set-camera-on-player/render)
    (cdq.render.clear-screen/render)
    (cdq.render.tiled-map/render)
    (cdq.render.draw-on-world-view/render)
    (cdq.render.draw-stage/render)
    (cdq.render.update-stage/render)
    (cdq.render.player-state-input/render)
    (cdq.render.update-mouseover-entity/render)
    (cdq.render.update-paused/render)
    (cdq.render.when-not-paused/render)
    (cdq.render.remove-destroyed-entities/render)
    (cdq.render.camera-controls/render)
    (cdq.render.window-controls/render)])

(def state (atom nil))

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
