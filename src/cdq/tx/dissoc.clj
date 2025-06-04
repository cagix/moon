(ns cdq.tx.dissoc
  (:require [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/dissoc [[_ eid k] _ctx]
  (swap! eid dissoc k)
  nil)
