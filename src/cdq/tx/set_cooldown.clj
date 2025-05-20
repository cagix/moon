(ns cdq.tx.set-cooldown
  (:require [cdq.timer :as timer]))

(defn do! [{:keys [ctx/elapsed-time]} eid skill]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create elapsed-time (:skill/cooldown skill))))
