(ns cdq.tx.pay-mana-cost
  (:require [cdq.stats :as stats]))

(defn do! [[_ eid cost] _ctx]
  (swap! eid update :creature/stats stats/pay-mana-cost cost)
  nil)
