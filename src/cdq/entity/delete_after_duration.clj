(ns cdq.entity.delete-after-duration
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]
            [cdq.utils :refer [defmethods]]))

(defn tick! [[_ counter] eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/mark-destroyed eid]]))

(defmethods :entity/delete-after-duration
  (entity/create [[_ duration] {:keys [ctx/elapsed-time]}]
    (timer/create elapsed-time duration)))
