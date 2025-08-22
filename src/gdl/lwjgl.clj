(ns gdl.lwjgl
  (:import (org.lwjgl.system Configuration)))

(defn set-glfw-library-name! [value]
  (.set Configuration/GLFW_LIBRARY_NAME value))
