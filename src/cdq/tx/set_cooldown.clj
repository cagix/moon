(ns cdq.tx.set-cooldown
  (:require [cdq.timer :as timer]))

(defn do! [[_ eid skill] {:keys [ctx/elapsed-time]}]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create elapsed-time (:skill/cooldown skill)))
  nil)
