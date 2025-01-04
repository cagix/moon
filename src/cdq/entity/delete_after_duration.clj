(ns cdq.entity.delete-after-duration
  (:require [gdl.context.timer :as timer]))

(defn tick [[_ counter] eid c]
  (when (timer/stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)))
