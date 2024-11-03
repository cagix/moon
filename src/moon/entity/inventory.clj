(ns moon.entity.inventory
  (:require [gdl.utils :refer [find-first]]
            [moon.item :as item]))

(defn- applies-modifiers? [[slot _]]
  (not= :inventory.slot/bag slot))

(defn- set-item [eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (item/valid-slot? cell item)))
    [[:e/assoc-in eid (cons :entity/inventory cell) item]
     (when (applies-modifiers? cell)
       [:entity/modifiers eid :add (:entity/modifiers item)])
     (when (:entity/player? entity)
       [:widgets/inventory :set cell item])]))

(defn- remove-item [eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    [[:e/assoc-in eid (cons :entity/inventory cell) nil]
     (when (applies-modifiers? cell)
       [:entity/modifiers eid :remove (:entity/modifiers item)])
     (when (:entity/player? entity)
       [:widgets/inventory :remove cell])]))

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

(defn- try-put-item-in [eid slot item]
  (let [inventory (:entity/inventory @eid)
        cells-items (item/cells-and-items inventory slot)
        [cell _cell-item] (find-first (fn [[_cell cell-item]] (item/stackable? item cell-item))
                                      cells-items)]
    (if cell
      (stack-item eid cell item)
      (when-let [[empty-cell] (find-first (fn [[_cell item]] (nil? item))
                                          cells-items)]
        (set-item eid empty-cell item)))))

(defn- pickup-item [eid item]
  (or
   (try-put-item-in eid (:item/slot item)   item)
   (try-put-item-in eid :inventory.slot/bag item)))

(defn can-pickup-item? [eid item]
  (boolean (pickup-item eid item)))

(defn create [[k items] eid]
  (cons [:e/assoc eid k item/empty-inventory]
        (for [item items]
          [k :pickup eid item])))

(defn handle [[_ op & args]]
  (case op
    :set    (apply set-item    args)
    :remove (apply remove-item args)
    :stack  (apply stack-item  args)
    :pickup (apply pickup-item args)))
