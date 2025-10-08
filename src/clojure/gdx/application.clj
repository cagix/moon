(ns clojure.gdx.application
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio])
  (:import (com.badlogic.gdx ApplicationListener
                             Audio
                             Files
                             Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.graphics Color
                                      Colors)
           (org.lwjgl.system Configuration)))

(extend-type Sound
  audio/Sound
  (play! [this]
    (.play this))
  (dispose! [this]
    (.dispose this)))

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
           resize!
           colors]}]
  (doseq [[name [r g b a]] colors]
    (Colors/put name (Color. r g b a)))
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
