(ns cdq.render.set-camera-on-player
  (:require [cdq.g :as g]))

(defn do! [{:keys [ctx/player-eid] :as ctx}]
  (g/set-camera-position! ctx (:position @player-eid))
  nil)
