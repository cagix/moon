(ns clojure.lwjgl
  (:import (org.lwjgl.system Configuration)))

(defn set-glfw-async! []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
