(ns cdq.entity.string-effect
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]
            [cdq.utils :refer [defmethods]]))

(defmethods :entity/string-effect
  (entity/tick! [[k {:keys [counter]}] eid {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
      [[:tx/dissoc eid k]])))
