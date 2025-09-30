(ns cdq.tx.pay-mana-cost
  (:require [cdq.entity.stats :as stats]))

(defn do! [_ctx eid cost]
  (swap! eid update :entity/stats stats/pay-mana-cost cost)
  nil)
