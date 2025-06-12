(ns clojure.lwjgl
  (:import (org.lwjgl.system Configuration)))

(defn set-glfw-library-name! [library-name-str]
  (.set Configuration/GLFW_LIBRARY_NAME library-name-str))
