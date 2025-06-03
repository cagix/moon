(ns cdq.tx.mod-remove
  (:require [cdq.entity :as entity]
            [cdq.ctx.effect-handler :refer [do!]]))

(defmethod do! :tx/mod-remove [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-remove modifiers))
