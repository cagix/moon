(ns cdq.application.config
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3ApplicationConfiguration)))

(defn create [{:keys [title window fps]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setWindowedMode (:width window)
                      (:height window))
    (.setForegroundFPS fps)))
