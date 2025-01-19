(ns cdq.application
  (:require cdq.application.create
            cdq.graphics
            cdq.utils
            clojure.gdx.application
            clojure.gdx.backends.lwjgl
            clojure.java.io))

(def render-fns
  '[(cdq.content-grid/assoc-active-entities)
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

(def ^:private runnables (atom []))

(defn post-runnable [f]
  (swap! runnables conj f))

; TODO move everything inside here in local functions
; and build a code browser
; or put it under cdq/application/create/context
; cdq/application/render/foo/bar/baz
; cdq/application/resize/viewports
(defn -main []
  (.setIconImage (java.awt.Taskbar/getTaskbar)
                 (.getImage (java.awt.Toolkit/getDefaultToolkit)
                            (clojure.java.io/resource "moon.png")))
  (when (= com.badlogic.gdx.utils.SharedLibraryLoader/os
           com.badlogic.gdx.utils.Os/MacOsX)
    (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (clojure.gdx.backends.lwjgl/application (reify clojure.gdx.application/Listener
                                            (create [_]
                                              (reset! state (cdq.application.create/context)))

                                            (dispose [_]
                                              ;(game/dispose @state)
                                              (doseq [[k value] @state
                                                      :when (cdq.utils/disposable? value)]
                                                ;(println "Disposing " k " - " value)
                                                (cdq.utils/dispose value)))

                                            (pause [_])

                                            (render [_]
                                              (when (seq @runnables)
                                                (println "Execute " (count @runnables) "runnables.")
                                                (swap! state (fn [context]
                                                               (reduce (fn [context f] (f context))
                                                                       context
                                                                       @runnables)))
                                                (reset! runnables []))
                                              (swap! state #_game/render

                                                     (fn [context]
                                                             (reduce (fn [context fn-invoc]
                                                                       (cdq.utils/req-resolve-call fn-invoc context))
                                                                     context
                                                                     render-fns))))

                                            (resize [_ width height]
                                              #_(game/resize @state width height)
                                              (cdq.graphics/resize-viewports @state width height))

                                            (resume [_]))
                                          {:title "Cyber Dungeon Quest"
                                           :windowed-mode {:width 1440
                                                           :height 900}
                                           :foreground-fps 60
                                           :opengl-emulation {:gl-version :gl20
                                                              :gles-3-major-version 3
                                                              :gles-3-minor-version 2}}))
