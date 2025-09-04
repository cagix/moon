(ns cdq.entity.state.stunned
  (:require [cdq.timer :as timer]))

(defn tick! [{:keys [counter]} eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/event eid :effect-wears-off]]))

(defn create [_eid duration {:keys [ctx/elapsed-time]}]
  {:counter (timer/create elapsed-time duration)})
