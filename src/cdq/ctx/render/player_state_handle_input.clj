(ns cdq.ctx.render.player-state-handle-input
  (:require [clojure.txs :as txs]
            [cdq.entity.state :as state]))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state/handle-input [state-k (state-k entity)] eid ctx)]
    (txs/handle! ctx txs))
  ctx)
