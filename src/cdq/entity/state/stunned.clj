(ns cdq.entity.state.stunned
  (:require [cdq.timer :as timer]))

(defn create [_eid duration {:keys [world/elapsed-time]}]
  {:counter (timer/create elapsed-time duration)})
