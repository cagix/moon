(ns com.badlogic.gdx.backends.lwjgl3.init.audio
  (:import (com.badlogic.gdx.backends.lwjgl3.audio.mock MockAudio)))

(defn do!
  [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application
           ^com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration init/config]
    :as init}]
  (if (.disableAudio config)
    (set! (.audio application) (MockAudio.))
    (try
     (set! (.audio application) (.createAudio application config))
     (catch Throwable t
       (.log application "Lwjgl3Application" "Couldn't initialize audio, disabling audio" t)
       (set! (.audio application) (MockAudio.)))))
  init)
