(ns cdq.tx.remove-item
  (:require [cdq.entity :as entity]
            [cdq.inventory :as inventory]))

(defn do! [_ctx eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      ((:item-removed! (:entity/player? entity)) cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))))
