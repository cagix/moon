(ns cdq.entity.delete-after-duration
  (:require [cdq.timer :as timer]
            [gdl.utils :as utils]))

(defn create [duration {:keys [world/elapsed-time]}]
  (timer/create elapsed-time duration))

(defn tick [counter eid {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/mark-destroyed eid]]))

(defn info-text [[_ counter] {:keys [world/elapsed-time]}]
  (str "Remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
