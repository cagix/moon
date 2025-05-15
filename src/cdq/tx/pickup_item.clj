(ns cdq.tx.pickup-item
  (:require [cdq.entity.inventory :as inventory]
            [cdq.tx :as tx]))

(defn do! [eid item]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (tx/stack-item eid cell item)
      (tx/set-item eid cell item))))
