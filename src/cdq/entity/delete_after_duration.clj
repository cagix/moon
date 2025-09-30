(ns cdq.entity.delete-after-duration
  (:require [cdq.timer :as timer]))

(defn create [duration {:keys [world/elapsed-time]}]
  (timer/create elapsed-time duration))

(defn tick [counter eid {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/mark-destroyed eid]]))
