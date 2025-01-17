(ns clojure.gdx.backends.lwjgl
  (:require [clojure.gdx.backends.lwjgl-config :as config]
            [clojure.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3ApplicationConfiguration$GLEmulation
                                             Lwjgl3ApplicationLogger
                                             Lwjgl3Clipboard
                                             Lwjgl3NativesLoader
                                             Lwjgl3Net
                                             Lwjgl3Window
                                             Sync)
           (com.badlogic.gdx.backends.lwjgl3.audio.mock MockAudio)
           (com.badlogic.gdx.utils GdxRuntimeException)))

(defn- create [listener config]
  (when (= (.glEmulation config) Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
    (println "Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20; " Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
    (Lwjgl3Application/loadANGLE))
  (Lwjgl3Application/initializeGlfw)
  (let [this (proxy [Lwjgl3Application] [])
        config (Lwjgl3ApplicationConfiguration/copy config)]
    (.setApplicationLogger this (Lwjgl3ApplicationLogger.))
    (set! (.config this) config)
    (set! Gdx/app this)
    (set! (.audio this) (if (.disableAudio config)
                          (MockAudio.)
                          (try (.createAudio this config)
                               (catch Throwable t
                                 (.log this "Lwjgl3Application", "Couldn't initialize audio, disabling audio", t)
                                 (MockAudio.)))))
    (set! Gdx/audio (.audio this))
    (set! (.files this) (.createFiles this))
    (set! Gdx/files (.files this))
    (set! (.clipboard this) (Lwjgl3Clipboard.))
    (set! (.sync this) (Sync.))
    (let [window (.createWindow this config listener 0)]
      (when (= (.glEmulation config) Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
        (Lwjgl3Application/postLoadANGLE))
      (.add (.windows this) window))
    (try
     (.loop this)
     (.cleanupWindows this)
     (catch Throwable t
       (if (instance? RuntimeException t)
         (throw t)
         (throw (GdxRuntimeException. t))))
     (finally
      (.cleanup this)))))

(defn application [{:keys [config
                           listener]}]
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (utils/req-resolve-call (:create listener)))

                        (dispose []
                          (utils/req-resolve-call (:dispose listener)))

                        (render []
                          (utils/req-resolve-call (:render listener)))

                        (resize [width height]
                          (utils/req-resolve-call (:resize listener) width height)))
                      (config/create config)))

(defn window
  "Creates a new Lwjgl3Window using the provided listener and Lwjgl3WindowConfiguration. This function only just instantiates a Lwjgl3Window and returns immediately. The actual window creation is postponed with Application.postRunnable(Runnable) until after all existing windows are updated."
  [application listener window-config]
  (Lwjgl3Application/.newWindow application listener window-config))


(defn set-gl-debug-message-control
  "Enables or disables GL debug messages for the specified severity level. Returns false if the severity level could not be set (e.g. the NOTIFICATION level is not supported by the ARB and AMD extensions). See Lwjgl3ApplicationConfiguration.enableGLDebugOutput(boolean, PrintStream)"
  [severity enabled?]
  (Lwjgl3Application/setGLDebugMessageControl severity #_(k->severity severity)
                                              (boolean enabled?)))


(defn display-mode
  "the currently active Graphics.DisplayMode of the primary or the given monitor"
  ([monitor]
   (Lwjgl3ApplicationConfiguration/getDisplayMode monitor))
  ([]
   (Lwjgl3ApplicationConfiguration/getDisplayMode)))

(defn display-modes
  "the available Graphics.DisplayModes of the primary or the given monitor"
  ([monitor]
   (Lwjgl3ApplicationConfiguration/getDisplayModes monitor))
  ([]
   (Lwjgl3ApplicationConfiguration/getDisplayModes)))

(defn primary-monitor
  "the primary Graphics.Monitor"
  []
  (Lwjgl3ApplicationConfiguration/getPrimaryMonitor))

(defn monitors
  "the connected Graphics.Monitors"
  []
  (Lwjgl3ApplicationConfiguration/getMonitors))
