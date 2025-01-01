(ns gdl.app
  (:require [clojure.component :as component]
            [clojure.gdx :as gdx]
            [clojure.gdx.lwjgl :as lwjgl]))

(def state (atom nil))

(defn start [app-config components render]
  (lwjgl/start app-config
               (reify lwjgl/Application
                 (create [_]
                   (reset! state (component/safe-create-into (gdx/context) components)))

                 (dispose [_]
                   (run! component/dispose @state))

                 (render [_]
                   (swap! state render))

                 (resize [_ width height]
                   (run! #(component/resize % width height) @state)))))
