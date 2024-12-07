(ns forge.entity.inventory
  (:require [clojure.utils :refer [find-first]]
            [forge.entity.modifiers :as mods]
            [forge.ui.inventory :as inventory :refer [valid-slot?]]
            [forge.world :refer [e-create]]))

(defn- applies-modifiers? [[slot _]]
  (not= :inventory.slot/bag slot))

(defn set-item [eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (valid-slot? cell item)))
    (when (:entity/player? entity)
      (inventory/set-item-image-in-widget cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (applies-modifiers? cell)
      (swap! eid mods/add (:entity/modifiers item)))))

(defn remove-item [eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (inventory/remove-item-from-widget cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (applies-modifiers? cell)
      (swap! eid mods/remove (:entity/modifiers item)))))

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

(defn stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; this is not required but can be asserted, all of one name should have count if others have count
       (= (:property/id item-a) (:property/id item-b))))

; TODO no items which stack are available
(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item eid cell)
            (set-item eid cell (update cell-item :count + (:count item))))))

(defn- cells-and-items [inventory slot]
  (for [[position item] (slot inventory)]
    [[slot position] item]))

(defn- free-cell [inventory slot item]
  (find-first (fn [[_cell cell-item]]
                (or (stackable? item cell-item)
                    (nil? cell-item)))
              (cells-and-items inventory slot)))

(defn can-pickup-item? [{:keys [entity/inventory]} item]
  (or
   (free-cell inventory (:item/slot item)   item)
   (free-cell inventory :inventory.slot/bag item)))

(defn pickup-item [eid item]
  (let [[cell cell-item] (can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (stackable? item cell-item)
                (nil? cell-item)))
    (if (stackable? item cell-item)
      (stack-item eid cell item)
      (set-item   eid cell item))))

(defmethod e-create :entity/inventory [[k items] eid]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (pickup-item eid item)))
