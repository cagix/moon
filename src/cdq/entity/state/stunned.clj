(ns cdq.entity.state.stunned
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]
            [cdq.utils :refer [defmethods]]))

(defn tick! [{:keys [counter]} eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/event eid :effect-wears-off]]))

(defmethods :stunned
  (entity/create [[_ _eid duration] {:keys [ctx/elapsed-time]}]
    {:counter (timer/create elapsed-time duration)}))
