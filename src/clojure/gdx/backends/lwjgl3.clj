(ns clojure.gdx.backends.lwjgl3
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)))

(defprotocol Listener
  (create  [_])
  (dispose [_])
  (render  [_])
  (resize  [_ w h]))

(defn app [listener config]
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create  []    (create  listener))
                        (dispose []    (dispose listener))
                        (render  []    (render  listener))
                        (resize  [w h] (resize  listener w h)))
                      config))

(defn config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))
