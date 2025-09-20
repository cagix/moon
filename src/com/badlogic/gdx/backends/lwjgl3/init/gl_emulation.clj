(ns com.badlogic.gdx.backends.lwjgl3.init.gl-emulation
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationConfiguration
                                             Lwjgl3ApplicationConfiguration$GLEmulation)
           (com.badlogic.gdx.utils GdxRuntimeException)))

(defn- load-angle! []
  (try
   (eval '(com.badlogic.gdx.backends.lwjgl3.angle.ANGLELoader/load))
   (catch ClassNotFoundException _
     nil)
   (catch Throwable t
     (throw (GdxRuntimeException. "Couldn't load ANGLE." t)))))

(defn- post-load-angle! []
  (try
   (eval '(com.badlogic.gdx.backends.lwjgl3.angle.ANGLELoader/postGlfwInit))
   (catch ClassNotFoundException _
     nil)
   (catch Throwable t
     (throw (GdxRuntimeException. "Couldn't load ANGLE." t)))))

(defn before-glfw
  [{:keys [^Lwjgl3ApplicationConfiguration init/config]
    :as init}]
  (when (= (.glEmulation config) Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
    (load-angle!))
  init)

(defn after-window-creation
  [{:keys [^Lwjgl3ApplicationConfiguration init/config]
    :as init}]
  (when (= (.glEmulation config) Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
    (post-load-angle!))
  init)
