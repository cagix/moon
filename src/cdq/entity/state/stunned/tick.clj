(ns cdq.entity.state.stunned.tick
  (:require [gdl.timer :as timer]))

(defn txs [{:keys [counter]} eid {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/event eid :effect-wears-off]]))
