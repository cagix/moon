(ns cdq.entity.delete-after-duration
  (:require [cdq.context :refer [timer stopped? finished-ratio]]
            [clojure.utils :refer [readable-number]]))

(defn info [counter c]
  (str "Remaining: " (readable-number (finished-ratio c counter)) "/1"))

(defn create [[_ duration] c]
  (timer c duration))

(defn tick [[_ counter] eid c]
  (when (stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)))
