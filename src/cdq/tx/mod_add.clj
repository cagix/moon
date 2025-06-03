(ns cdq.tx.mod-add
  (:require [cdq.entity :as entity]
            [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/mod-add [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-add modifiers))
