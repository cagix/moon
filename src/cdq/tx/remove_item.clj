(ns cdq.tx.remove-item
  (:require [cdq.ui.windows.inventory :as inventory-window]
            [cdq.inventory :as inventory]
            [cdq.world.entity.stats :as modifiers]))

(defn- remove-item! [{:keys [ctx/stage]} inventory-cell]
  (-> stage
      :windows
      :inventory-window
      (inventory-window/remove-item! inventory-cell)))

(defn do! [[_ eid cell] ctx]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid update :creature/stats modifiers/remove (:entity/modifiers item)))
    (when (:entity/player? entity)
      (remove-item! ctx cell))
    nil))
