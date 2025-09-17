(ns init.glfw
  (:require [com.badlogic.gdx.backends.lwjgl3.natives-loader :as natives-loader]
            [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]
            [org.lwjgl.glfw :as glfw]
            [org.lwjgl.glfw.error-callback :as error-callback])
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils GdxRuntimeException)
           (org.lwjgl.glfw GLFW)))

(defn do! [init]
  (natives-loader/load!)
  (let [error-callback (error-callback/create-print Lwjgl3ApplicationConfiguration/errorStream)]
    (glfw/set-error-callback! error-callback)
    (when (= (shared-library-loader/operating-system) :mac)
      (GLFW/glfwInitHint GLFW/GLFW_ANGLE_PLATFORM_TYPE
                         GLFW/GLFW_ANGLE_PLATFORM_TYPE_METAL))
    (GLFW/glfwInitHint GLFW/GLFW_JOYSTICK_HAT_BUTTONS,
                       GLFW/GLFW_FALSE)
    (when-not (GLFW/glfwInit)
      (throw (GdxRuntimeException. "Unable to initialize GLFW")))
    (assoc init :init/error-callback error-callback)))
