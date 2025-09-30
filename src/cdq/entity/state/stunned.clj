(ns cdq.entity.state.stunned
  (:require [cdq.timer :as timer]))

(defn create [_eid duration {:keys [world/elapsed-time]}]
  {:counter (timer/create elapsed-time duration)})

(defn tick [{:keys [counter]} eid {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/event eid :effect-wears-off]]))
