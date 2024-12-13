(ns anvil.entity.alert-friendlies-after-duration
  (:require [anvil.component :as component]
            [anvil.entity.fsm :as fsm]
            [anvil.world :refer [stopped? friendlies-in-radius]]
            [gdl.utils :refer [defmethods]]))

(defmethods :entity/alert-friendlies-after-duration
  (component/tick [[_ {:keys [counter faction]}] eid]
    (when (stopped? counter)
      (swap! eid assoc :entity/destroyed? true)
      (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
        (fsm/event friendly-eid :alert)))))
