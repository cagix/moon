(ns forge.entity.delete-after-duration
  (:require [clojure.utils :refer [defmethods]]
            [forge.entity :refer [->v tick]]
            [forge.world.time :refer [stopped?]]))

(defmethods :entity/delete-after-duration
  (->v [[_ duration]]
    (timer duration))

  (tick [[_ counter] eid]
    (when (stopped? counter)
      (swap! eid assoc :entity/destroyed? true))))
