(ns clojure.gdx.backends.lwjgl
  (:require [clojure.utils :as utils])
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

    ; FIXMe
    (set! (.audio this) (MockAudio.))

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

(defn application [{:keys [title
                           window-width
                           window-height
                           foreground-fps
                           listener]}]
  (create             (proxy [ApplicationAdapter] []
                        (create []
                          (utils/req-resolve-call (:create listener)))

                        (dispose []
                          (utils/req-resolve-call (:dispose listener)))

                        (render []
                          (utils/req-resolve-call (:render listener)))

                        (resize [width height]
                          (utils/req-resolve-call (:resize listener) width height)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode window-width
                                          window-height)
                        (.setForegroundFPS foreground-fps))))
