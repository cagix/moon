(ns cdq.entity.alert-friendlies-after-duration
  (:require [cdq.context :as world :refer [friendlies-in-radius]]
            [gdl.context.timer :as timer]))

(defn tick [[_ {:keys [counter faction]}] eid c]
  (when (timer/stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius c (:position @eid) faction)]
      (world/send-event! c friendly-eid :alert))))
