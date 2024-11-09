(ns moon.entity.inventory
  (:require [gdl.system :refer [*k*]]
            [gdl.utils :refer [find-first]]
            [moon.item :as item]
            [moon.widgets.inventory :as inventory]))

(defn- applies-modifiers? [[slot _]]
  (not= :inventory.slot/bag slot))

(defn- set-item [eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (item/valid-slot? cell item)))
    (when (:entity/player? entity)
      (inventory/set-item-image-in-widget cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (applies-modifiers? cell)
      [[:entity/modifiers eid :add (:entity/modifiers item)]])))

(defn- remove-item [eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (inventory/remove-item-from-widget cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (applies-modifiers? cell)
      [[:entity/modifiers eid :remove (:entity/modifiers item)]])))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! eid cell)
       (set-item! eid cell (update item :count dec)))
      (remove-item! eid cell))))

; TODO no items which stack are available
(defn- stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (item/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item eid cell)
            (set-item eid cell (update cell-item :count + (:count item))))))

(defn- free-cell [inventory slot item]
  (find-first (fn [[_cell cell-item]]
                (or (item/stackable? item cell-item)
                    (nil? cell-item)))
              (item/cells-and-items inventory slot)))

(defn can-pickup-item? [{:keys [entity/inventory]} item]
  (or
   (free-cell inventory (:item/slot item)   item)
   (free-cell inventory :inventory.slot/bag item)))

(defn- pickup-item [eid item]
  (let [[cell cell-item] (can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (item/stackable? item cell-item)
                (nil? cell-item)))
    (if (item/stackable? item cell-item)
      (stack-item eid cell item)
      (set-item   eid cell item))))

(defn create [items eid]
  (swap! eid assoc *k* item/empty-inventory)
  (mapv #(vector *k* :pickup eid %) items))

(defn handle [op & args]
  (case op
    :set    (apply set-item    args)
    :remove (apply remove-item args)
    :stack  (apply stack-item  args)
    :pickup (apply pickup-item args)))
