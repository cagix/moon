(ns cdq.entity.string-effect.tick
  (:require [cdq.timer :as timer]))

(defn txs
  [{:keys [counter]}
   eid
   {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc eid :entity/string-effect]]))
