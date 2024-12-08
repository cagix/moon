(ns forge.entity.delete-after-duration
  (:require [forge.world.time :refer [timer stopped?]]))

(defn ->v [[_ duration]]
  (timer duration))

(defn tick [[_ counter] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)))
