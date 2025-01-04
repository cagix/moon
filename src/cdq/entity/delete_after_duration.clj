(ns cdq.entity.delete-after-duration
  (:require [cdq.context :refer [timer stopped?]]))

(defn create [[_ duration] c]
  (timer c duration))

(defn tick [[_ counter] eid c]
  (when (stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)))
