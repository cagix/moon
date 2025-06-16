(ns cdq.entity.string-effect
  (:require [cdq.timer :as timer]))

(defn tick! [{:keys [counter]} eid {:keys [ctx/world]}]
  (when (timer/stopped? (:world/elapsed-time world) counter)
    [[:tx/dissoc eid :entity/string-effect]]))
