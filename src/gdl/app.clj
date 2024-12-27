(ns gdl.app
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]))

(defprotocol Context
  (dispose [_] "Releases all resources of the context.")
  (render [_] "Renders a frame.")
  (resize [_ width height] "Called when window was resized."))

(def state (atom nil))

(defn start [app-config create-context]
  (lwjgl/start app-config
               (reify lwjgl/Application
                 (create [_]
                   (reset! state (create-context (gdx/context))))

                 (dispose [_]
                   (dispose @state))

                 (render [_]
                   (swap! state render))

                 (resize [_ width height]
                   (resize @state width height)))))
