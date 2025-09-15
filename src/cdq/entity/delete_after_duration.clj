(ns cdq.entity.delete-after-duration
  (:require [cdq.timer :as timer]
            [clojure.utils :as utils]))

(defn create [duration {:keys [ctx/world]}]
  (timer/create (:world/elapsed-time world) duration))

(defn tick! [counter eid {:keys [ctx/world]}]
  (when (timer/stopped? (:world/elapsed-time world) counter)
    [[:tx/mark-destroyed eid]]))

(defn info-text [[_ counter] {:keys [ctx/world]}]
  (str "Remaining: " (utils/readable-number (timer/ratio (:world/elapsed-time world) counter)) "/1"))
