(ns cdq.game.set-glfw-async
  (:import (org.lwjgl.system Configuration)))

(defn do! []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
