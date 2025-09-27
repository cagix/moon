(ns cdq.tx.set-item
  (:require [cdq.entity.inventory :as inventory]
            [cdq.stats :as stats]))

(defn do! [_ctx eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid update :entity/stats stats/add (:entity/modifiers item)))
    (if (:entity/player? entity)
      [[:tx/player-set-item cell item]]
      nil)))
