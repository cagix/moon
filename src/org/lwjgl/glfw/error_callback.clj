(ns org.lwjgl.glfw.error-callback
  (:import (org.lwjgl.glfw GLFWErrorCallback)))

(defn create-print [x]
  (GLFWErrorCallback/createPrint x))
