(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.context :as world :refer [stopped? friendlies-in-radius]]))

(defn tick [[_ {:keys [counter faction]}] eid c]
  (when (stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius c (:position @eid) faction)]
      (world/send-event! c friendly-eid :alert))))
