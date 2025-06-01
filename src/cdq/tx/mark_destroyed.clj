(ns cdq.tx.mark-destroyed
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/mark-destroyed [[_ eid] _ctx]
  (swap! eid assoc :entity/destroyed? true))
