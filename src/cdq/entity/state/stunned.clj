(ns cdq.entity.state.stunned
  (:require [cdq.timer :as timer]))

(defn create [_eid duration {:keys [ctx/world]}]
  {:counter (timer/create (:world/elapsed-time world) duration)})
