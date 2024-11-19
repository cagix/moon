(ns moon.entity.delete-after-duration
  (:require [gdl.utils :refer [readable-number]]
            [moon.world :refer [timer stopped? finished-ratio]]))

(defn ->v [duration]
  (timer duration))

(defn info [counter]
  (str "[LIGHT_GRAY]Remaining: " (readable-number (finished-ratio counter)) "/1[]"))

(defn tick [counter eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)))

