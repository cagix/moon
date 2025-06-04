(ns cdq.tx.set-item
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]))

(defmethod do! :tx/set-item [[_ eid cell item] ctx]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))
    (when (:entity/player? entity)
      [:world.event/player-item-set [cell item]])))
