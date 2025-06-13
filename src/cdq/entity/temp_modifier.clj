(ns cdq.entity.temp-modifier
  (:require [cdq.timer :as timer]))

(defn tick! [[k {:keys [modifiers counter]}]
             eid
             {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc eid k]
     [:tx/mod-remove eid modifiers]]))
