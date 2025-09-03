(ns cdq.tx.pickup-item
  (:require [cdq.ctx :as ctx]
            [cdq.inventory :as inventory]))

(defn do! [[_ eid item] ctx]
  (inventory/assert-valid-item? item)
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (do
       #_(tx/stack-item ctx eid cell item))
      (ctx/do! [:tx/set-item eid cell item] ctx))))
