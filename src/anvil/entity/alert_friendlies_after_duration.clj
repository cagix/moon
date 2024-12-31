(ns ^:no-doc anvil.entity.alert-friendlies-after-duration
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [cdq.context :refer [stopped? friendlies-in-radius]]
            [clojure.utils :refer [defmethods]]))

(defmethods :entity/alert-friendlies-after-duration
  (component/tick [[_ {:keys [counter faction]}] eid c]
    (when (stopped? c counter)
      (swap! eid assoc :entity/destroyed? true)
      (doseq [friendly-eid (friendlies-in-radius c (:position @eid) faction)]
        (entity/event c friendly-eid :alert)))))
