(ns cdq.render.player-state-handle-input
  (:require [cdq.ctx :as ctx]
            [cdq.entity.state :as state]))

(defn do!
  [{:keys [ctx/player-eid]
    :as ctx}]
  (let [handle-input (state/state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  nil)
