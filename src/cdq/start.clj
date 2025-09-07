(ns cdq.start
  (:require cdq.application
            cdq.application.listener
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
  (lwjgl/start-application! (cdq.application.listener/create cdq.application/state
                                                             (cdq.ctx.listener/create))
                            {:title "Cyber Dungeon Quest"
                             :windowed-mode {:width 1440 :height 900}
                             :foreground-fps 60}))

(defn -main []
  (os-specific-settings!)
  (start-lwjgl-application!))
