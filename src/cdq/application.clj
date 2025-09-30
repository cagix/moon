(ns cdq.application
  (:require [cdq.c :as c]
            [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [org.lwjgl.system.configuration :as lwjgl-system])
  (:import (com.badlogic.gdx ApplicationListener))
  (:gen-class))

(def config '{
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
  (let [dispose (requiring-resolve (:dispose config))
        render-pipeline (map requiring-resolve (:render config))
        resize (requiring-resolve (:resize config))]
    (lwjgl-system/set-glfw-library-name! "glfw_async")
    (lwjgl/application (reify ApplicationListener
                         (create [_]
                           (reset! state (c/create! {:ctx/audio    (gdx/audio)
                                                     :ctx/files    (gdx/files)
                                                     :ctx/graphics (gdx/graphics)
                                                     :ctx/input    (gdx/input)})))
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
