(ns cdq.entity.delete-after-duration
  (:require [cdq.timer :as timer]
            [cdq.utils :as utils]))

(defn create [duration {:keys [ctx/elapsed-time]}]
  (timer/create elapsed-time duration))

(defn tick! [counter eid {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/mark-destroyed eid]]))

(defn info-text [[_ counter] {:keys [ctx/elapsed-time]}]
  (str "Remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
