(ns moon.entity.delete-after-duration
  (:require [gdl.utils :refer [readable-number]]
            [moon.world.time :refer [timer stopped? finished-ratio]]))

(defn ->v [[_ duration]]
  (timer duration))

(defn info [[_ counter]]
  (str "[LIGHT_GRAY]Remaining: " (readable-number (finished-ratio counter)) "/1[]"))

(defn tick [[_ counter] eid]
  (when (stopped? counter)
    [[:e/destroy eid]]))

