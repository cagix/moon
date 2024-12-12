(ns clojure.lwjgl
  (:import (org.lwjgl.system Configuration)))

(defn configure-glfw-for-mac []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (.set Configuration/GLFW_CHECK_THREAD0 false))
