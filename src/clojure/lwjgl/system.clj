(ns clojure.lwjgl.system
  (:import (org.lwjgl.system Configuration)))

(defn set-glfw-library-name [name]
  (.set Configuration/GLFW_LIBRARY_NAME name))
