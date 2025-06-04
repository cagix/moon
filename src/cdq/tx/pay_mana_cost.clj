(ns cdq.tx.pay-mana-cost
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.entity :as entity]))

(defmethod do! :tx/pay-mana-cost [[_ eid cost] _ctx]
  (swap! eid entity/pay-mana-cost cost)
  nil)
