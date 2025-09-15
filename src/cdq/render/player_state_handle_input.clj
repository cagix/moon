(ns cdq.render.player-state-handle-input
  (:require [cdq.ctx :as ctx]))

(defn do!
  [{:keys [ctx/entity-states
           ctx/world]
    :as ctx}]
  (let [handle-input ((:handle-input entity-states) (:state (:entity/fsm @(:world/player-eid world))))
        txs (if handle-input
              (handle-input (:world/player-eid world) ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  nil)
