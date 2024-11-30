(ns ^:no-doc forge.entity.player.item-on-cursor
  (:require [forge.graphics :refer [draw-centered gui-mouse-position world-mouse-position]]
            [forge.stage :as stage]
            [forge.entity.components :as entity]
            [forge.entity.inventory :as inventory]
            [forge.item :refer [valid-slot? stackable?]]
            [forge.world :as world]))

(defn- clicked-cell [eid cell]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (valid-slot? cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (inventory/set-item eid cell item-on-cursor)
      (entity/event eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (stackable? item-in-cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (inventory/stack-item eid cell item-on-cursor)
      (entity/event eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (valid-slot? cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (inventory/remove-item eid cell)
      (inventory/set-item eid cell item-on-cursor)
      (entity/event eid :dropped-item)
      (entity/event eid :pickup-item item-in-cell)))))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.

(defn- placement-point [player target maxrange]
  (v-add player
         (v-scale (v-direction player target)
                  (min maxrange
                       (v-distance player target)))))

(defn item-place-position [entity]
  (placement-point (:position entity)
                   (world-mouse-position)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn world-item? []
  (not (stage/mouse-on-actor?)))

(defn ->v [eid item]
  {:eid eid
   :item item})

(defn pause-game? [_]
  true)

(defn manual-tick [{:keys [eid]}]
  (when (and (button-just-pressed? :left)
             (world-item?))
    (entity/event eid :drop-item)))

(defn clicked-inventory-cell [{:keys [eid]} cell]
  (clicked-cell eid cell))

(defn cursor [_]
  :cursors/hand-grab)

(defn enter [{:keys [eid item]}]
  (swap! eid assoc :entity/item-on-cursor item))

(defn exit [{:keys [eid]}]
  ; at clicked-cell when we put it into a inventory-cell
  ; we do not want to drop it on the ground too additonally,
  ; so we dissoc it there manually. Otherwise it creates another item
  ; on the ground
  (let [entity @eid]
    (when (:entity/item-on-cursor entity)
      (play-sound "bfxr_itemputground")
      (swap! eid dissoc :entity/item-on-cursor)
      (world/item (item-place-position entity) (:entity/item-on-cursor entity)))))

(defn draw-gui-view [{:keys [eid]}]
  (let [entity @eid]
    (when (and (= :player-item-on-cursor (entity/state-k entity))
               (not (world-item?)))
      (draw-centered (:entity/image (:entity/item-on-cursor entity))
                     (gui-mouse-position)))))
