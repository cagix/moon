(ns forge.application
  (:require [clojure.awt :as awt]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defprotocol Listener
  (create [_])
  (dispose [_])
  (render [_])
  (resize [_ w h]))

(defn start [listener {:keys [dock-icon glfw lwjgl3]}]
  (awt/set-dock-icon dock-icon)
  (lwjgl/set-glfw-config glfw)
  (lwjgl3/start-app (proxy [ApplicationAdapter] []
                      (create  []   (create   listener))
                      (dispose []   (dispose  listener))
                      (render  []   (render   listener))
                      (resize [w h] (resize   listener w h)))
                    lwjgl3))
