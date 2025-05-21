(ns cdq.tx.set-item
  (:require [cdq.entity :as entity]
            [cdq.inventory :as inventory]))

(defn do! [ctx eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (when (:entity/player? entity)
      ((:item-set! (:entity/player? entity)) ctx cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))
