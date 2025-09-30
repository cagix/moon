(ns cdq.entity.temp-modifier
  (:require [cdq.timer :as timer]))

(defn tick
  [{:keys [modifiers counter]}
   eid
   {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc     eid :entity/temp-modifier]
     [:tx/mod-remove eid modifiers]]))
