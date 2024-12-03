(ns clojure.lwjgl
  (:import (org.lwjgl.system Configuration)))

(defn set-glfw-config [{:keys [library-name check-thread0]}]
  (.set Configuration/GLFW_LIBRARY_NAME library-name)
  (.set Configuration/GLFW_CHECK_THREAD0 check-thread0))
