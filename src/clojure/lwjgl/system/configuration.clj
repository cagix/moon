(ns clojure.lwjgl.system.configuration
  (:import (org.lwjgl.system Configuration)))

(defn GLFW_LIBRARY_NAME [setting]
  (println "SET GLFW_LIBRARY_NAME " setting)
  (.set Configuration/GLFW_LIBRARY_NAME setting))
