(ns cdq.tx.set-item
  (:require [cdq.inventory :as inventory]
            [cdq.stats :as stats]))

(defn do! [[_ eid cell item] _ctx]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid update :creature/stats stats/add (:entity/modifiers item)))
    (if (:entity/player? entity)
      [[:tx/player-set-item cell item]]
      nil)))
