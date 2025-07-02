(ns cdq.render.player-state-handle-input
  (:require [cdq.ctx :as ctx]))

(defn do! [{:keys [ctx/world]
            :as ctx}
           {:keys [state->handle-input]}]
  (let [player-eid (:world/player-eid world)
        handle-input (state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  ctx)
