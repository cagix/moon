(ns cdq.entity.delete-after-duration
  (:require [cdq.timer :as timer]))

(defn tick! [counter eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/mark-destroyed eid]]))

(defn create [duration {:keys [ctx/elapsed-time]}]
  (timer/create elapsed-time duration))
