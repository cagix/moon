(ns cdq.entity.delete-after-duration.tick
  (:require [gdl.timer :as timer]))

(defn txs [counter eid {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/mark-destroyed eid]]))
