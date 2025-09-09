(ns cdq.entity.delete-after-duration
  (:require [cdq.timer :as timer]))

(defn create [duration {:keys [ctx/elapsed-time]}]
  (timer/create elapsed-time duration))
