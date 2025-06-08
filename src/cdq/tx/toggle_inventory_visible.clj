(ns cdq.tx.toggle-inventory-visible
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/toggle-inventory-visible [_ _ctx]
  [:world.event/toggle-inventory-visible])
