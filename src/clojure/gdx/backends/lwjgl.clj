(ns clojure.gdx.backends.lwjgl
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration
                                             Lwjgl3ApplicationConfiguration$GLEmulation
                                             Lwjgl3ApplicationLogger
                                             Lwjgl3Clipboard
                                             Lwjgl3Net
                                             Lwjgl3WindowConfiguration
                                             Sync)
           (com.badlogic.gdx.backends.lwjgl3.audio.mock MockAudio)))

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

(defn- create-listener
  [{:keys [create!
           dispose!
           render!
           resize!
           pause!
           resume!]}]
  (reify ApplicationListener
    (create [_]
      (create!))
    (dispose [_]
      (dispose!))
    (render [_]
      (render!))
    (resize [_ width height]
      (resize! width height))
    (pause [_]
      (pause!))
    (resume [_]
      (resume!))))

(defn- gl-emulation-hook [gl-emulation]
  (when (= gl-emulation
           Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20)
    (Lwjgl3Application/loadANGLE)))

(defn start-application! [listener config]
  (let [application (Lwjgl3Application.)
        listener (create-listener listener)
        config (Lwjgl3ApplicationConfiguration/copy (create-config config))]
    (gl-emulation-hook (.glEmulation config))
    (Lwjgl3Application/initializeGlfw)
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
    (.start application
            listener
            config)))

(defn start!
  [{:keys [ctx/application-state]
    :as ctx}
   {:keys [listener
           config]}]
  (reset! application-state ctx)
  (start-application! (let [[f params] listener]
                        (f params))
                      config))
