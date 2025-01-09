(ns cdq.app
  (:require [gdl.app :as app]
            [gdl.utils :as utils]
            [cdq.game :as game])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [config (utils/read-edn-resource "cdq.app.edn")]
    (app/start (:app config)
               (reify app/Listener
                 (create [_ context]
                   (reset! state (game/create! (assoc context ::config config))))

                 (dispose [_]
                   (swap! state game/dispose!))

                 (render [_]
                   (swap! state game/render!))

                 (resize [_ width height]
                   (swap! state game/resize! width height))))))

(defn post-runnable [f]
  (app/post-runnable (:clojure.gdx/app @state)
                     #(f @state)))
