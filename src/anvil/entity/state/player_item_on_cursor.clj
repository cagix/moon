(ns ^:no-doc anvil.entity.state.player-item-on-cursor
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.world :as world]
            [gdl.context :refer [play-sound]]
            [gdl.graphics :as g]
            [gdl.stage :refer [mouse-on-actor?]]
            [gdl.math.vector :as v]))

(defn- world-item? []
  (not (mouse-on-actor?)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.

(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn- item-place-position [entity]
  (placement-point (:position entity)
                   (g/world-mouse-position)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn- clicked-cell [eid cell]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (entity/valid-slot? cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/set-item eid cell item-on-cursor)
      (entity/event eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (entity/stackable? item-in-cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/stack-item eid cell item-on-cursor)
      (entity/event eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (entity/valid-slot? cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/remove-item eid cell)
      (entity/set-item eid cell item-on-cursor)
      (entity/event eid :dropped-item)
      (entity/event eid :pickup-item item-in-cell)))))

(defmethods :player-item-on-cursor
  (component/->v [[_ eid item]]
    {:eid eid
     :item item})

  (component/enter [[_ {:keys [eid item]}]]
    (swap! eid assoc :entity/item-on-cursor item))

  (component/exit [[_ {:keys [eid]}]]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (play-sound "bfxr_itemputground")
        (swap! eid dissoc :entity/item-on-cursor)
        (world/item (item-place-position entity)
                    (:entity/item-on-cursor entity)))))

  (component/manual-tick [[_ {:keys [eid]}]]
    (when (and (button-just-pressed? :left)
               (world-item?))
      (entity/event eid :drop-item)))

  (component/render-below [[_ {:keys [item]}] entity]
    (when (world-item?)
      (g/draw-centered (:entity/image item)
                       (item-place-position entity))))

  (component/draw-gui-view [[_ {:keys [eid]}]]
    (when (not (world-item?))
      (g/draw-centered (:entity/image (:entity/item-on-cursor @eid))
                       (g/mouse-position))))

  (component/clicked-inventory-cell [[_ {:keys [eid]}] cell]
    (clicked-cell eid cell)))
