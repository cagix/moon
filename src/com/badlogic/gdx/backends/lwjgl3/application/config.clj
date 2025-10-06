(ns com.badlogic.gdx.backends.lwjgl3.application.config
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationConfiguration)))

(defn create [config]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setWindowedMode (:width (:windowed-mode config))
                      (:height (:windowed-mode config)))
    (.setTitle (:title config))
    (.setForegroundFPS (:foreground-fps config))))
