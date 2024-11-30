(ns ^:no-doc forge.entity.delete-after-duration
  (:require [moon.world :refer [timer stopped?]]))

(defn ->v [duration]
  (timer duration))

(defn tick [counter eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)))
