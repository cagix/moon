(ns cdq.entity.delete-after-duration
  (:require [cdq.timer :as timer]))

(defn create [duration {:keys [ctx/world]}]
  (timer/create (:world/elapsed-time world) duration))
