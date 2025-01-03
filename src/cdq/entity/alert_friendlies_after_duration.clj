(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.entity :as entity]
            [cdq.context :refer [stopped? friendlies-in-radius]]))

(defn tick [[_ {:keys [counter faction]}] eid c]
  (when (stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius c (:position @eid) faction)]
      (entity/event c friendly-eid :alert))))
