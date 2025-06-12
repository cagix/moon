(ns gdx.backends.lwjgl.application
  (:require [gdx.backends.lwjgl.application.config :as application-config])
  (:import (com.badlogic.gdx ApplicationAdapter
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn start! [config {:keys [create! dispose! render! resize!]}]
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (create! {:ctx/app      Gdx/app
                                    :ctx/audio    Gdx/audio
                                    :ctx/files    Gdx/files
                                    :ctx/graphics Gdx/graphics
                                    :ctx/input    Gdx/input}))

                        (dispose []
                          (dispose!))

                        (render []
                          (render!))

                        (resize [width height]
                          (resize! width height)))
                      (application-config/create config)))
