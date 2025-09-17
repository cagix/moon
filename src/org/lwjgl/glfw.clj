(ns org.lwjgl.glfw
  (:import (org.lwjgl.glfw GLFW)))

(defn set-error-callback! [cb]
  (GLFW/glfwSetErrorCallback cb))
