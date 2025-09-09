(ns cdq.tx.remove-item
  (:require [cdq.inventory :as inventory]
            [cdq.stats :as stats]))

(defn do! [[_ eid cell] _ctx]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid update :creature/stats stats/remove (:entity/modifiers item)))
    (if (:entity/player? entity)
      [[:tx/player-remove-item cell]]
      nil)))
