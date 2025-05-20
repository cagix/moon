(ns cdq.tx.pickup-item
  (:require [cdq.inventory :as inventory]
            [cdq.tx.set-item]))

(defn do! [ctx eid item]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (do
       #_(tx/stack-item ctx eid cell item))
      (cdq.tx.set-item/do! ctx eid cell item))))
