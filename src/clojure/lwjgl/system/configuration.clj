(ns clojure.lwjgl.system.configuration
  (:import (org.lwjgl.system Configuration)))

(defn set-glfw-library-name! [library-name]
  (.set Configuration/GLFW_LIBRARY_NAME library-name))
