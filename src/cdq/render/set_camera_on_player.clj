(ns cdq.render.set-camera-on-player
  (:require [cdq.g :as g]
            [cdq.entity :as entity]))

(defn do! [{:keys [ctx/player-eid]
            :as ctx}]
  (g/set-camera-position! ctx (entity/position @player-eid))
  ctx)
