(ns clojure.lwjgl
  (:import (org.lwjgl.system Configuration)))

(defn configure [{:keys [glfw-library-name glfw-check-thread0]}]
  (.set Configuration/GLFW_LIBRARY_NAME  glfw-library-name)
  (.set Configuration/GLFW_CHECK_THREAD0 glfw-check-thread0))
