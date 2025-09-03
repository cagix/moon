(ns cdq.tx.set-item
  (:require [cdq.ctx :refer [do!] :as ctx]
            [cdq.graphics :as graphics]
            [cdq.ui.windows.inventory :as inventory-window]
            [cdq.inventory :as inventory]
            [cdq.world.entity.stats :as modifiers]))

(defn- set-item!
  [{:keys [ctx/graphics
           ctx/stage]
    :as ctx}
   inventory-cell item]
  (-> stage
      :windows
      :inventory-window
      (inventory-window/set-item! inventory-cell
                                  {:texture-region (graphics/image->texture-region graphics (:entity/image item))
                                   :tooltip-text (ctx/info-text ctx item)})))

(defn do! [[_ eid cell item] ctx]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid update :creature/stats modifiers/add (:entity/modifiers item)))
    (when (:entity/player? entity)
      (set-item! ctx cell item))
    nil))
