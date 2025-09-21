(ns cdq.entity.temp-modifier
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]))

(defn tick!
  [{:keys [modifiers counter]}
   eid
   {:keys [ctx/world]}]
  (when (timer/stopped? (:world/elapsed-time world) counter)
    [[:tx/dissoc eid :entity/temp-modifier]
     [:tx/mod-remove eid modifiers]]))
