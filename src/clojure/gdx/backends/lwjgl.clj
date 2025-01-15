(ns clojure.gdx.backends.lwjgl
  (:require [clojure.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 CljLwjgl3Application
                                             Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3ApplicationConfiguration$GLEmulation
                                             Lwjgl3ApplicationLogger
                                             Lwjgl3Clipboard
                                             Lwjgl3NativesLoader
                                             Lwjgl3Net
                                             Lwjgl3Window
                                             Mync)
           (com.badlogic.gdx.backends.lwjgl3.audio.mock MockAudio)
           (com.badlogic.gdx.utils GdxRuntimeException)))

#_(defn create [listener config]
  ; config.glEmulation private field !
  ; copy whole backend folder& move deps with deps tree in my dir??
  #_(when (= (.glEmulation config) Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
      (println "Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20; " Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
      (CljLwjgl3Application/loadANGLE))
  (CljLwjgl3Application/initializeGlfw)
  (let [this (proxy [CljLwjgl3Application] [])
        config config #_(Lwjgl3ApplicationConfiguration/copy config)]
    (.setApplicationLogger this (Lwjgl3ApplicationLogger.))
    (set! (.config this) config)
    (set! Gdx/app this)
    (set! (.audio this) (MockAudio.))
    (set! Gdx/audio (.audio this))
    (set! (.files this) (.createFiles this))
    (set! Gdx/files (.files this))
    (set! (.clipboard this) (Lwjgl3Clipboard.))
    (set! (.sync this) (Mync.))
    (let [window (.createWindow this config listener 0)]
      #_(when (= (.glEmulation config) Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
          (CljLwjgl3Application/postLoadANGLE))
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
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
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
