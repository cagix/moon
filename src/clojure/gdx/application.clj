(ns clojure.gdx.application
  (:require [clojure.gdx :as gdx])
  (:import (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration)))

(defrecord Context [^Audio audio
                    ^Files files
                    graphics
                    input]
  gdx/Audio
  (sound [_ path]
    (.newSound audio (.internal files path)))
  )

(defn start!
  [{:keys [title
           window
           fps
           create!
           dispose!
           render!
           resize!]}]
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (create! (map->Context
                                    {:audio    Gdx/audio
                                     :files    Gdx/files
                                     :graphics Gdx/graphics
                                     :input    Gdx/input})))

                        (dispose [_]
                          (dispose!))

                        (render [_]
                          (render!))

                        (resize [_ width height]
                          (resize! width height))

                        (pause [_])

                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setWindowedMode (:width window)
                                          (:height window))
                        (.setForegroundFPS fps))))
