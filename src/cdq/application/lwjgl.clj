(ns cdq.application.lwjgl
  (:require cdq.application
            cdq.ctx.listener
            [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start! []
  (lwjgl/start-application! {:create! (fn []
                                        (reset! cdq.application/state (cdq.ctx.listener/create)))
                             :dispose! (fn []
                                         (cdq.ctx.listener/dispose @cdq.application/state))
                             :render! (fn []
                                        (swap! cdq.application/state cdq.ctx.listener/render))
                             :resize! (fn [width height]
                                        (cdq.ctx.listener/resize @cdq.application/state width height))
                             :pause! (fn [])
                             :resume! (fn [])}
                            {:title "Cyber Dungeon Quest"
                             :windowed-mode {:width 1440 :height 900}
                             :foreground-fps 60}))
