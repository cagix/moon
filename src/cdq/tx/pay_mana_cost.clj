(ns cdq.tx.pay-mana-cost
  (:require [cdq.entity :as entity]))

(defn- pay-mana-cost [entity cost]
  (let [mana-val ((entity/mana entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))

(defn do! [eid cost]
  (swap! eid pay-mana-cost cost))
