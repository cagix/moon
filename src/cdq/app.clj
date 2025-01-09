(ns cdq.app
  (:require [gdl.app :as app]
            [cdq.game :as game])
  (:gen-class))

(def state (atom nil))

(comment

 (clojure.pprint/pprint
  (sort (keys @state)))

 )

(defn post-runnable [f]
  (app/post-runnable @state #(f @state)))

(defrecord Listener []
  app/Listener
  (create  [this config]       (game/create!  this config))
  (dispose [this]              (game/dispose! this))
  (render  [this]              (game/render!  this))
  (resize  [this width height] (game/resize!  this width height)))

(defn -main []
  (app/start state (Listener.)))
