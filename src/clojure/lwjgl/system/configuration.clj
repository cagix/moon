(ns clojure.lwjgl.system.configuration
  (:import (org.lwjgl.system Configuration)))

(defn glfw-library-name [setting]
  (.set Configuration/GLFW_LIBRARY_NAME setting))
