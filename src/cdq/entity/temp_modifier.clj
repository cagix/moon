(ns cdq.entity.temp-modifier
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]))

(defn tick
  [{:keys [modifiers counter]}
   eid
   {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc     eid :entity/temp-modifier]
     [:tx/mod-remove eid modifiers]]))

(defn draw [_ entity _ctx]
  [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]])
