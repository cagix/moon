(ns cdq.application
  (:require cdq.scene2d.build.editor-overview-window
            cdq.scene2d.build.editor-window
            [cdq.ctx.create-db :as create-db]
            [cdq.ctx.create-audio :as create-audio]
            [cdq.ctx.create-graphics :as create-graphics]
            [cdq.ctx.create-record :as create-record]
            [cdq.ctx.create-stage :as create-stage]
            [cdq.ctx.create-input :as create-input]
            [cdq.ctx.create-vis-ui :as create-vis-ui]
            [cdq.ctx.create-world :as create-world]
            [cdq.ctx.dispose :as dispose]
            [cdq.ctx.dissoc-files :as dissoc-files]
            [cdq.ctx.update-viewports :as update-viewports]
            [cdq.ctx.assoc-active-entities :as assoc-active-entities]
            [cdq.ctx.assoc-paused :as assoc-paused]
            [cdq.ctx.assoc-interaction-state :as assoc-interaction-state]
            [cdq.ctx.clear-screen :as clear-screen]
            [cdq.ctx.check-open-debug :as check-open-debug]
            [cdq.ctx.dissoc-interaction-state :as dissoc-interaction-state]
            [cdq.ctx.draw-on-world-viewport :as draw-on-world-viewport]
            [cdq.ctx.draw-world-map :as draw-world-map]
            [cdq.ctx.get-stage-ctx :as get-stage-ctx]
            [cdq.ctx.render-stage :as render-stage]
            [cdq.ctx.build-stage-actors :as build-stage-actors]
            [cdq.ctx.remove-destroyed-entities :as remove-destroyed-entities]
            [cdq.ctx.set-camera-on-player :as set-camera-on-player]
            [cdq.ctx.set-cursor :as set-cursor]
            [cdq.ctx.tick-entities :as tick-entities]
            [cdq.ctx.player-state-handle-input :as player-state-handle-input]
            [cdq.ctx.update-mouse :as update-mouse]
            [cdq.ctx.update-mouseover-eid :as update-mouseover-eid]
            [cdq.ctx.update-potential-fields :as update-potential-fields]
            [cdq.ctx.update-world-time :as update-world-time]
            [cdq.ctx.validate :as validate]
            [cdq.ctx.window-camera-controls :as window-camera-controls]
            [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [org.lwjgl.system.configuration :as lwjgl-system])
  (:import (com.badlogic.gdx ApplicationListener))
  (:gen-class))

(def state (atom nil))

(defn -main []
  (lwjgl-system/set-glfw-library-name! "glfw_async")
  (lwjgl/application (reify ApplicationListener
                       (create [_]
                         (reset! state (-> {:ctx/audio    (gdx/audio)
                                            :ctx/files    (gdx/files)
                                            :ctx/graphics (gdx/graphics)
                                            :ctx/input    (gdx/input)}
                                           create-record/do!
                                           create-db/do!
                                           create-graphics/do!
                                           create-vis-ui/do!
                                           create-stage/do!
                                           build-stage-actors/do!
                                           create-input/do!
                                           create-audio/do!
                                           dissoc-files/do!
                                           (create-world/do! "world_fns/vampire.edn"))))
                       (dispose [_]
                         (dispose/do! @state))
                       (render [_]
                         (swap! state (fn [ctx]
                                        (-> ctx
                                            get-stage-ctx/do!
                                            validate/do!
                                            update-mouse/do!
                                            update-mouseover-eid/do!
                                            check-open-debug/do!
                                            assoc-active-entities/do!
                                            set-camera-on-player/do!
                                            clear-screen/do!
                                            draw-world-map/do!
                                            draw-on-world-viewport/do!
                                            assoc-interaction-state/do!
                                            set-cursor/do!
                                            player-state-handle-input/do!
                                            dissoc-interaction-state/do!
                                            assoc-paused/do!
                                            update-world-time/do!
                                            update-potential-fields/do!
                                            tick-entities/do!
                                            remove-destroyed-entities/do!
                                            window-camera-controls/do!
                                            render-stage/do!
                                            validate/do!))))
                       (resize [_ width height]
                         (update-viewports/do! @state width height))
                       (pause [_])
                       (resume [_]))
                     {:title "Cyber Dungeon Quest"
                      :windowed-mode {:width 1440
                                      :height 900}
                      :foreground-fps 60}))
