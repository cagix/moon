(ns anvil.entity.inventory
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.widgets :as widgets]
            [data.grid2d :as g2d]))

(bind-root entity/empty-inventory
  (->> #:inventory.slot{:bag      [6 4]
                        :weapon   [1 1]
                        :shield   [1 1]
                        :helm     [1 1]
                        :chest    [1 1]
                        :leg      [1 1]
                        :glove    [1 1]
                        :boot     [1 1]
                        :cloak    [1 1]
                        :necklace [1 1]
                        :rings    [2 1]}
       (map (fn [[slot [width height]]]
              [slot (g2d/create-grid width
                                     height
                                     (constantly nil))]))
       (into {})))

(defn-impl entity/valid-slot? [[slot _] item]
  (or (= :inventory.slot/bag slot)
      (= (:item/slot item) slot)))

(defn- applies-modifiers? [[slot _]]
  (not= :inventory.slot/bag slot))

(defn-impl entity/set-item [eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (entity/valid-slot? cell item)))
    (when (:entity/player? entity)
      (widgets/set-item-image-in-widget cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))

(defn-impl entity/remove-item [eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (widgets/remove-item-from-widget cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn-impl entity/remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! eid cell)
       (set-item! eid cell (update item :count dec)))
      (remove-item! eid cell))))

(defn-impl entity/stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; this is not required but can be asserted, all of one name should have count if others have count
       (= (:property/id item-a) (:property/id item-b))))

; TODO no items which stack are available
(defn-impl entity/stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (entity/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (entity/remove-item eid cell)
            (entity/set-item eid cell (update cell-item :count + (:count item))))))

(defn- cells-and-items [inventory slot]
  (for [[position item] (slot inventory)]
    [[slot position] item]))

(defn- free-cell [inventory slot item]
  (find-first (fn [[_cell cell-item]]
                (or (entity/stackable? item cell-item)
                    (nil? cell-item)))
              (cells-and-items inventory slot)))

(defn-impl entity/can-pickup-item? [{:keys [entity/inventory]} item]
  (or
   (free-cell inventory (:item/slot item)   item)
   (free-cell inventory :inventory.slot/bag item)))

(defn-impl entity/pickup-item [eid item]
  (let [[cell cell-item] (entity/can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (entity/stackable? item cell-item)
                (nil? cell-item)))
    (if (entity/stackable? item cell-item)
      (entity/stack-item eid cell item)
      (entity/set-item   eid cell item))))

(defmethods :entity/inventory
  (component/create [[k items] eid]
    (swap! eid assoc k entity/empty-inventory)
    (doseq [item items]
      (entity/pickup-item eid item))))
