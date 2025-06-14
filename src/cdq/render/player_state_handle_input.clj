(ns cdq.render.player-state-handle-input
  (:require [cdq.world :as world]))

(defn do! [{:keys [ctx/player-eid]
            :as ctx}
           {:keys [state->handle-input]}]
  (let [handle-input (state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (world/handle-txs! ctx txs))
  ctx)
