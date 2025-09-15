(ns cdq.render.player-state-handle-input
  (:require [cdq.ctx :as ctx]
            [cdq.entity.state :as state]))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state/handle-input [state-k (state-k entity)]
                                eid
                                ctx)]
    (ctx/handle-txs! ctx txs))
  ctx)
