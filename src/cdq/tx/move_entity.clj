(ns cdq.tx.move-entity
  (:require [cdq.w :as w]))

(defn do! [[_ & opts] {:keys [ctx/world]}]
  (apply w/move-entity! world opts)
  nil)
