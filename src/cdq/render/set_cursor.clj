(ns cdq.render.set-cursor
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.graphics :as g]))

(defn do!
  [{:keys [ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (let [cursor-key (state/cursor (entity/state-obj @player-eid)
                                 player-eid
                                 ctx)]
    (g/set-cursor! graphics cursor-key))
  ctx)

