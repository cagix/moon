(ns cdq.entity.delete-after-duration
  (:require [cdq.timer :as timer]))

(defn tick! [counter eid {:keys [ctx/world]}]
  (when (timer/stopped? (:world/elapsed-time world) counter)
    [[:tx/mark-destroyed eid]]))

(defn create [duration {:keys [ctx/world]}]
  (timer/create (:world/elapsed-time world) duration))
