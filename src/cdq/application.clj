(ns cdq.application
  (:require [cdq.game :as game])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration))
  (:gen-class))

(def state (atom nil))

(defn -main []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (reset! state (game/create! {:audio    Gdx/audio
                                                       :files    Gdx/files
                                                       :graphics Gdx/graphics
                                                       :input    Gdx/input})))

                        (dispose [_]
                          (game/dispose! @state))

                        (render [_]
                          (swap! state game/render!))

                        (resize [_ width height]
                          (game/resize! @state width height))

                        (pause [_])

                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle "Cyber Dungeon Quest")
                        (.setWindowedMode 1440 900)
                        (.setForegroundFPS 60))))
