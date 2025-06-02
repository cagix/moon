(ns clojure.tx.pay-mana-cost
  (:require [clojure.ctx.effect-handler :refer [do!]]
            [clojure.entity :as entity]))

(defmethod do! :tx/pay-mana-cost [[_ eid cost] _ctx]
  (swap! eid entity/pay-mana-cost cost))
