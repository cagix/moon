(ns cdq.entity.state.stunned
  (:require [cdq.timer :as timer]))

(defn tick! [{:keys [counter]} eid {:keys [ctx/world]}]
  (when (timer/stopped? (:world/elapsed-time world) counter)
    [[:tx/event eid :effect-wears-off]]))

(defn create [_eid duration {:keys [ctx/world]}]
  {:counter (timer/create (:world/elapsed-time world) duration)})
