(ns cdq.application.os-specific-settings
  (:require [clojure.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn handle! []
  (->> (shared-library-loader/operating-system)
       {:mac '[[clojure.lwjgl.system.configuration/set-glfw-library-name! "glfw_async"]
               [clojure.java.awt/set-taskbar-icon! "icon.png"]]}
       (run! (fn [[f params]]
               ((requiring-resolve f) params)))))
