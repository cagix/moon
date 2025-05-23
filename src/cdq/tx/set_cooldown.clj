(ns cdq.tx.set-cooldown
  (:require [cdq.g :as g]))

(defn do! [ctx eid skill]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (g/create-timer ctx (:skill/cooldown skill))))
