(ns cdq.entity.string-effect
  (:require [cdq.timer :as timer]))

(defn tick
  [{:keys [counter]}
   eid
   {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc eid :entity/string-effect]]))
