(ns cdq.app
  (:require [gdl.app :as app]
            [gdl.utils :as utils]
            [cdq.game :as game])
  (:gen-class))

(defrecord Listener []
  gdl.app/Listener
  (create [this context config]
    (game/create! (merge this context) config))

  (dispose [this]
    (game/dispose! this))

  (render [this]
    (game/render! this))

  (resize [this width height]
    (game/resize! this width height)))

(def state (atom nil))

(defn -main []
  (app/start state
             (Listener.)
             (utils/read-edn-resource "config.edn")))

(defn post-runnable [f]
  (app/post-runnable @state #(f @state)))
