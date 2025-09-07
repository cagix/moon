(ns cdq.start
  (:require cdq.application
            cdq.ctx.listener
            [cdq.core :as core]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader])
  (:gen-class))

(defn- os-specific-settings! []
  (->> (shared-library-loader/operating-system)
       {:mac '[[clojure.lwjgl.system.configuration/set-glfw-library-name! "glfw_async"]
               [clojure.java.awt/set-taskbar-icon! "icon.png"]]}
       (run! core/execute!)))

(defn- start-lwjgl-application! []
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

(defn -main []
  (os-specific-settings!)
  (start-lwjgl-application!))
