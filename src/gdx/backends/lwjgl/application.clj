(ns gdx.backends.lwjgl.application
  (:require [gdx.backends.lwjgl.application.config :as application-config])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn start! [[config {:keys [create! dispose! render! resize!]}]]
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (let [[f params] create!]
                            (f params)))

                        (dispose []
                          (dispose!))

                        (render []
                          (let [[f params] render!]
                            (f params)))

                        (resize [width height]
                          (resize! width height)))
                      (application-config/create config)))
