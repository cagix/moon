(ns cdq.ctx.player-state-handle-input
  (:require [cdq.ctx.handle-txs :as handle-txs]
            [cdq.entity.state :as state]))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state/handle-input [state-k (state-k entity)] eid ctx)]
    (handle-txs/do! ctx txs))
  ctx)
