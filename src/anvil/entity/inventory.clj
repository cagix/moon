(ns anvil.entity.inventory
  (:require [cdq.inventory :as inventory :refer [empty-inventory]]
            [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.widgets :as widgets]))

(defn-impl entity/set-item [c eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (when (:entity/player? entity)
      (widgets/set-item-image-in-widget c cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))

(defn-impl entity/remove-item [c eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (widgets/remove-item-from-widget c cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
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

; TODO no items which stack are available
(defn-impl entity/stack-item [c eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (entity/remove-item c eid cell)
            (entity/set-item c eid cell (update cell-item :count + (:count item))))))

(defn-impl entity/can-pickup-item? [{:keys [entity/inventory]} item]
  (or
   (inventory/free-cell inventory (:item/slot item)   item)
   (inventory/free-cell inventory :inventory.slot/bag item)))

(defn-impl entity/pickup-item [c eid item]
  (let [[cell cell-item] (entity/can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (entity/stack-item c eid cell item)
      (entity/set-item c eid cell item))))

(defmethods :entity/inventory
  (component/create [[k items] eid c]
    (swap! eid assoc k empty-inventory)
    (doseq [item items]
      (entity/pickup-item c eid item))))
