(ns cdq.tx.set-cooldown
  (:require [cdq.ctx :as ctx]
            [cdq.timer :as timer]))

(defn do! [eid skill]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create ctx/elapsed-time (:skill/cooldown skill))))
