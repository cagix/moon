(ns cdq.app
  (:require [clojure.gdx.backends.lwjgl3.application :as lwjgl3-app]
            [cdq.app.listener :as listener])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(def state (atom nil))

(defn start [config]
  (lwjgl3-app/create (proxy [ApplicationAdapter] []
                       (create []
                         (reset! state (listener/create (:context config))))

                       (dispose []
                         (listener/dispose @state))

                       (render []
                         (swap! state listener/render))

                       (resize [width height]
                         (listener/resize @state width height)))
                     (:lwjgl3 config)))
