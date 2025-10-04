(ns cdq.entity.state.stunned
  (:require [clojure.timer :as timer]))

(defn create [_eid duration {:keys [world/elapsed-time]}]
  {:counter (timer/create elapsed-time duration)})
