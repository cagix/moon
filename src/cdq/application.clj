(ns cdq.application
  (:require [cdq.game.create :as create]
            [cdq.game.dispose :as dispose]
            [cdq.game.render :as render]
            [cdq.game.resize :as resize])
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
                          (reset! state (create/do! {:audio    Gdx/audio
                                                     :files    Gdx/files
                                                     :graphics Gdx/graphics
                                                     :input    Gdx/input})))

                        (dispose [_]
                          (dispose/do! @state))

                        (render [_]
                          (swap! state render/do!))

                        (resize [_ width height]
                          (resize/do! @state width height))

                        (pause [_])

                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle "Cyber Dungeon Quest")
                        (.setWindowedMode 1440 900)
                        (.setForegroundFPS 60))))
