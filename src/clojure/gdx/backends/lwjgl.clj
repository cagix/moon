(ns clojure.gdx.backends.lwjgl
  (:require [clojure.utils :as utils])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)))

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
