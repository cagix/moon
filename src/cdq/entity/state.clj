(ns cdq.entity.state
  (:require [anvil.entity :as entity]
            [cdq.context :as world]
            [cdq.inventory :as inventory]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.gdx :refer [play]]
            [gdl.context :as c]))

(defcomponent :player-idle
  (component/clicked-inventory-cell [[_ {:keys [eid player-idle/pickup-item-sound]}] cell c]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      (play pickup-item-sound)
      (entity/event c eid :pickup-item item)
      (entity/remove-item c eid cell))))

(defn- clicked-cell [{:keys [player-item-on-cursor/item-put-sound]} eid cell c]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/set-item c eid cell item-on-cursor)
      (entity/event c eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/stack-item c eid cell item-on-cursor)
      (entity/event c eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (play item-put-sound)
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/remove-item c eid cell)
      (entity/set-item c eid cell item-on-cursor)
      (entity/event c eid :dropped-item)
      (entity/event c eid :pickup-item item-in-cell)))))

(defcomponent :player-item-on-cursor
  (component/draw-gui-view [[_ {:keys [eid]}] c]
    (when (not (world/world-item? c))
      (c/draw-centered c
                       (:entity/image (:entity/item-on-cursor @eid))
                       (c/mouse-position c))))

  (component/clicked-inventory-cell [[_ {:keys [eid] :as data}] cell c]
    (clicked-cell data eid cell c)))
