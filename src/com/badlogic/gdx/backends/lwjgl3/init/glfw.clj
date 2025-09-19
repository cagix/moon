(ns com.badlogic.gdx.backends.lwjgl3.init.glfw
  (:require [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationConfiguration
                                             Lwjgl3NativesLoader)
           (com.badlogic.gdx.utils GdxRuntimeException)
           (org.lwjgl.glfw GLFW
                           GLFWErrorCallback)))

(defn do! [init]
  (Lwjgl3NativesLoader/load)
  (let [error-callback (GLFWErrorCallback/createPrint Lwjgl3ApplicationConfiguration/errorStream)]
    (GLFW/glfwSetErrorCallback error-callback)
    (when (= (shared-library-loader/operating-system) :mac)
      (GLFW/glfwInitHint GLFW/GLFW_ANGLE_PLATFORM_TYPE
                         GLFW/GLFW_ANGLE_PLATFORM_TYPE_METAL))
    (GLFW/glfwInitHint GLFW/GLFW_JOYSTICK_HAT_BUTTONS,
                       GLFW/GLFW_FALSE)
    (when-not (GLFW/glfwInit)
      (throw (GdxRuntimeException. "Unable to initialize GLFW")))
    (assoc init :init/error-callback error-callback)))
