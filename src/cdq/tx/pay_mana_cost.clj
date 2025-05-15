(ns cdq.tx.pay-mana-cost
  (:require [cdq.entity :as entity]))

(defn do! [eid cost]
  (swap! eid entity/pay-mana-cost cost))
