(ns cdq.render.player-state-handle-click
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.world :as world]))

(defn do! [{:keys [ctx/player-eid]
            :as ctx}]
  (world/handle-txs! ctx
                     (state/manual-tick (entity/state-obj @player-eid)
                                        player-eid
                                        ctx))
  ctx)
