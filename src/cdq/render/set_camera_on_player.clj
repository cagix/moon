(ns cdq.render.set-camera-on-player
  (:require [cdq.entity :as entity]
            [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics
                   ctx/player-eid]
            :as ctx}]
  (graphics/set-camera-position! graphics
                                 (entity/position @player-eid))
  ctx)
