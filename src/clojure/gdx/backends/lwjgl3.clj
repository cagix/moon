(ns clojure.gdx.backends.lwjgl3
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)))

(defn application [application-listener config]
  (Lwjgl3Application. application-listener
                      config))

(defn config [{:keys [title
                      window-width
                      window-height
                      foreground-fps]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setWindowedMode window-width
                      window-height)
    (.setForegroundFPS foreground-fps)))
