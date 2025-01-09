(ns cdq.app
  (:require [gdl.app :as app]
            [cdq.game :as game])
  (:gen-class))

(defrecord Listener []
  gdl.app/Listener
  (create [this config]
    (game/create! this config))

  (dispose [this]
    (game/dispose! this))

  (render [this]
    (game/render! this))

  (resize [this width height]
    (game/resize! this width height)))

(def state (atom nil))

(comment

 (clojure.pprint/pprint
  (sort (keys @state)))

 )

(defn post-runnable [f]
  (app/post-runnable @state #(f @state)))

(defn -main []
  (app/start state
             (Listener.)
             "config.edn"))
