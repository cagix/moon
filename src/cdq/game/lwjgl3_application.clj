(ns cdq.game.lwjgl3-application
  (:require [cdq.game :as game])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)))

(defn do! [{:keys [title
                   window-width
                   window-height
                   fps
                   create
                   dispose
                   render
                   resize]}]
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (game/execute! create))

                        (dispose []
                          (game/execute! dispose))

                        (render []
                          (game/execute! render))

                        (resize [_width _height]
                          (game/execute! resize)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode window-width window-height)
                        (.setForegroundFPS fps))))
