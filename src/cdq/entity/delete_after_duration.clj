(ns cdq.entity.delete-after-duration
  (:require [clojure.timer :as timer]))

(defn create [duration {:keys [world/elapsed-time]}]
  (timer/create elapsed-time duration))
