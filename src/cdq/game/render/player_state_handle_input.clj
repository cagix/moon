(ns cdq.game.render.player-state-handle-input
  (:require [cdq.entity.state :as state]
            [clojure.txs :as txs]))

(defn step
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state/handle-input [state-k (state-k entity)] eid ctx)]
    (txs/handle! ctx txs))
  ctx)
