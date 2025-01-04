(ns cdq.entity.delete-after-duration
  (:require [gdl.context.timer :as timer]))

(defn create [[_ duration] c]
  (timer/create c duration))

(defn tick [[_ counter] eid c]
  (when (timer/stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)))
