(ns ^:no-doc anvil.entity.alert-friendlies-after-duration
  (:require [anvil.entity :as entity]
            [cdq.context :refer [stopped? friendlies-in-radius]]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :entity/alert-friendlies-after-duration
  (component/tick [[_ {:keys [counter faction]}] eid c]
    (when (stopped? c counter)
      (swap! eid assoc :entity/destroyed? true)
      (doseq [friendly-eid (friendlies-in-radius c (:position @eid) faction)]
        (entity/event c friendly-eid :alert)))))
