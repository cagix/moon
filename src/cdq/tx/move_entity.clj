(ns cdq.tx.move-entity
  (:require [cdq.world :as w]))

(defn do! [[_ & opts] {:keys [ctx/world]}]
  (apply w/move-entity! world opts)
  nil)
