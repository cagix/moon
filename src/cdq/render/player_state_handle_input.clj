(ns cdq.render.player-state-handle-input
  (:require [cdq.ctx :as ctx]))

(defn do!
  [{:keys [ctx/entity-states
           ctx/player-eid]
    :as ctx}]
  (let [handle-input ((:handle-input entity-states) (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  nil)
