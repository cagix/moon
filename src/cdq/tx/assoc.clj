(ns cdq.tx.assoc
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/assoc [[_ eid k value] _ctx]
  (swap! eid assoc k value))
