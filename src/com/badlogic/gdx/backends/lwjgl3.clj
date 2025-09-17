(ns com.badlogic.gdx.backends.lwjgl3
  (:require [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.backends.lwjgl3.natives-loader :as natives-loader]
            [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]
            [org.lwjgl.glfw :as glfw]
            [org.lwjgl.glfw.error-callback :as error-callback])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3ApplicationConfiguration$GLEmulation
                                             Lwjgl3ApplicationLogger
                                             Lwjgl3Clipboard
                                             Lwjgl3Net
                                             Lwjgl3WindowConfiguration
                                             Sync)
           (com.badlogic.gdx.backends.lwjgl3.audio.mock MockAudio)
           (com.badlogic.gdx.utils Array
                                   GdxRuntimeException)
           (org.lwjgl.glfw GLFW)))

(def error-callback nil)

(defn initializeGlfw []
  (when-not error-callback
    (natives-loader/load!)
    (.bindRoot #'error-callback (error-callback/create-print Lwjgl3ApplicationConfiguration/errorStream))
    (glfw/set-error-callback! error-callback)
    (when (= (shared-library-loader/operating-system) :mac)
      (GLFW/glfwInitHint GLFW/GLFW_ANGLE_PLATFORM_TYPE
                         GLFW/GLFW_ANGLE_PLATFORM_TYPE_METAL))
    (GLFW/glfwInitHint GLFW/GLFW_JOYSTICK_HAT_BUTTONS,
                       GLFW/GLFW_FALSE)
    (when-not (GLFW/glfwInit)
      (throw (GdxRuntimeException. "Unable to initialize GLFW")))))

(defn- set-window-config-key! [^Lwjgl3WindowConfiguration object k v]
  (case k
    :windowed-mode (.setWindowedMode object
                                     (int (:width v))
                                     (int (:height v)))
    :title (.setTitle object (str v))))

(defn- set-config-key! [^Lwjgl3ApplicationConfiguration object k v]
  (case k
    :foreground-fps (.setForegroundFPS object (int v))
    (set-window-config-key! object k v)))

(defn- create-config [config]
  (let [obj (Lwjgl3ApplicationConfiguration.)]
    (doseq [[k v] config]
      (set-config-key! obj k v))
    obj))

(defn- gl-emulation-hook [gl-emulation]
  (when (= gl-emulation
           Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
    (Lwjgl3Application/loadANGLE)))

(defn- gl-emulation-hook-after-window [gl-emulation]
  (when (= gl-emulation
           Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
    (Lwjgl3Application/postLoadANGLE)))

(defn start-application! [listener config]
  (let [application (Lwjgl3Application.)
        listener (gdx/application-listener listener)
        config (Lwjgl3ApplicationConfiguration/copy (create-config config))]
    (gl-emulation-hook (.glEmulation config))
    (initializeGlfw)
    (.setApplicationLogger application (Lwjgl3ApplicationLogger.))
    (set! (.config application) config)
    (when-not (.title config)
      (set! (.title config) (.getSimpleName (class listener))))
    (set! Gdx/app application)
    (if (.disableAudio config)
      (set! (.audio application) (MockAudio.))
      (try
       (set! (.audio application) (.createAudio application config))
       (catch Throwable t
         (.log application "Lwjgl3Application" "Couldn't initialize audio, disabling audio" t)
         (set! (.audio application) (MockAudio.)))))
    (set! Gdx/audio (.audio application))
    (set! (.files application) (.createFiles application))
    (set! Gdx/files (.files application))
    (set! (.net application) (Lwjgl3Net. config))
    (set! (.clipboard application) (Lwjgl3Clipboard.))
    (set! Gdx/net (.net application))
    (set! (.sync application) (Sync.))
    (let [window (.createWindow application config listener 0)]
      (gl-emulation-hook-after-window (.glEmulation config))
      (.add (.windows application) window))
    (try
     (let [closed-windows (Array.)]
       (while (and (.running application)
                   (> (.size (.windows application)) 0))
         (.update (.audio application)) ; FIXME put it on a separate thread
         (.loop application closed-windows)))
     (.cleanupWindows application)
     (catch Throwable t
       (throw t)
      ; if (t instanceof RuntimeException)
      ; throw (RuntimeException)t;
      ; else
      ; throw new GdxRuntimeException(t);
       )
     (finally
      (.free error-callback)
      (.bindRoot #'error-callback nil)
      (.cleanup application)))))
