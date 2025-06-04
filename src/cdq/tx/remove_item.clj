(ns cdq.tx.remove-item
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]))

(defmethod do! :tx/remove-item [[_ eid cell] ctx]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))
    (when (:entity/player? entity)
      [:world.event/player-item-removed cell])))
