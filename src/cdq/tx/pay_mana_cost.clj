(ns cdq.tx.pay-mana-cost
  (:require [cdq.world.entity.stats :as modifiers]))

(defn- pay-mana-cost [entity cost]
  (update entity :creature/stats modifiers/pay-mana-cost cost))

(defn do! [[_ eid cost] _ctx]
  (swap! eid pay-mana-cost cost)
  nil)
