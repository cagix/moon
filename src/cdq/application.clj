(ns cdq.application
  (:require [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [org.lwjgl.system.configuration :as lwjgl-system])
  (:import (com.badlogic.gdx ApplicationListener))
  (:gen-class))

(def config '{
              :create [cdq.application.create.record/do!
                       cdq.application.create.validation/do!
                       cdq.application.create.editor/do!
                       cdq.ui.editor.window/do!
                       cdq.application.create.handle-txs/do!
                       cdq.application.create.db/do!
                       cdq.application.create.vis-ui/do!
                       cdq.application.create.graphics/do!
                       cdq.application.create.stage/do!
                       cdq.application.create.input/do!
                       cdq.application.create.audio/do!
                       cdq.application.create.remove-files/do!
                       cdq.application.create.world/do!
                       cdq.application.create.reset-game-state/do!]
              :dispose cdq.application.dispose/do!
              :render  [cdq.application.render.try-fetch-state-ctx/do!
                        cdq.application.render.validate/do!
                        cdq.application.render.update-mouse/do!
                        cdq.application.render.update-mouseover-eid/do!
                        cdq.application.render.check-open-debug/do!
                        cdq.application.render.assoc-active-entities/do!
                        cdq.application.render.set-camera-on-player/do!
                        cdq.application.render.clear-screen/do!
                        cdq.application.render.draw-world-map/do!
                        cdq.application.render.draw-on-world-viewport/do!
                        cdq.application.render.assoc-interaction-state/do!
                        cdq.application.render.set-cursor/do!
                        cdq.application.render.player-state-handle-input/do!
                        cdq.application.render.remove-interaction-state/do!
                        cdq.application.render.assoc-paused/do!
                        cdq.application.render.update-world-time/do!
                        cdq.application.render.update-potential-fields/do!
                        cdq.application.render.tick-entities/do!
                        cdq.application.render.remove-destroyed-entities/do!
                        cdq.application.render.window-and-camera-controls/do!
                        cdq.application.render.render-stage/do!
                        cdq.application.render.validate/do!]
              :resize  cdq.application.resize/do!
              })

(def state (atom nil))

(defn -main []
  (let [create-pipeline (map requiring-resolve (:create config))
        dispose (requiring-resolve (:dispose config))
        render-pipeline (map requiring-resolve (:render config))
        resize (requiring-resolve (:resize config))]
    (lwjgl-system/set-glfw-library-name! "glfw_async")
    (lwjgl/application (reify ApplicationListener
                         (create [_]
                           (reset! state (reduce (fn [ctx f]
                                                   (f ctx))
                                                 {:ctx/audio    (gdx/audio)
                                                  :ctx/files    (gdx/files)
                                                  :ctx/graphics (gdx/graphics)
                                                  :ctx/input    (gdx/input)}
                                                 create-pipeline)))
                         (dispose [_]
                           (dispose @state))
                         (render [_]
                           (swap! state (fn [ctx]
                                          (reduce (fn [ctx f]
                                                    (f ctx))
                                                  ctx
                                                  render-pipeline))))
                         (resize [_ width height]
                           (resize @state width height))
                         (pause [_])
                         (resume [_]))
                       {:title "Cyber Dungeon Quest"
                        :windowed-mode {:width 1440
                                        :height 900}
                        :foreground-fps 60})))
