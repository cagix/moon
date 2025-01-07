(ns cdq.app
  (:require [clojure.gdx.backends.lwjgl3.application :as lwjgl3-app]
            [cdq.app.listener :as listener]))

(def state (atom nil))

(defn start [config]
  (lwjgl3-app/create (reify lwjgl3-app/Listener
                       (create [_ context]
                         (reset! state (listener/create context (:context config))))

                       (dispose [_]
                         (listener/dispose @state))

                       (render [_]
                         (swap! state listener/render))

                       (resize [_ width height]
                         (listener/resize @state width height)))
                     (:lwjgl3 config)))
