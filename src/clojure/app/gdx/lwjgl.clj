(ns clojure.app.gdx.lwjgl
  (:import (com.badlogic.gdx ApplicationListener)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)))

(defn start! [{:keys [create
                      dispose
                      render
                      resize
                      title
                      windowed-mode
                      foreground-fps
                      config-path]}]
  (Lwjgl3Application. (proxy [ApplicationListener] []
                        (create []
                          (let [[f params] create]
                            (f params)))
                        (dispose []
                          (dispose))
                        (render  []
                          (let [[f params] render]
                            (f params)))
                        (resize [width height]
                          (resize width height))
                        (pause [])
                        (resume []))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width  windowed-mode)
                                          (:height windowed-mode))
                        (.setForegroundFPS foreground-fps))))
