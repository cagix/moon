(ns cdq.entity.temp-modifier
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/temp-modifier
  (entity/tick! [[k {:keys [modifiers counter]}]
                 eid
                 {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
      [[:tx/dissoc eid k]
       [:tx/mod-remove eid modifiers]])))
