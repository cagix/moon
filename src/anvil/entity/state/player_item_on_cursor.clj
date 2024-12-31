(ns ^:no-doc anvil.entity.state.player-item-on-cursor
  (:require [anvil.entity :as entity]
            [cdq.context :as world]
            [cdq.inventory :as inventory]
            [clojure.gdx :refer [button-just-pressed? play]]
            [clojure.utils :refer [defmethods safe-merge]]
            [gdl.context :as c]
            [gdl.math.vector :as v]))

(defn- world-item? [c]
  (not (c/mouse-on-actor? c)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.

(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn- item-place-position [c entity]
  (placement-point (:position entity)
                   (c/world-mouse-position c)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

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

(defmethods :player-item-on-cursor
  (entity/->v [[_ eid item] c]
    (safe-merge (c/build c :player-item-on-cursor/component)
                {:eid eid
                 :item item}))

  (entity/enter [[_ {:keys [eid item]}] c]
    (swap! eid assoc :entity/item-on-cursor item))

  (entity/exit [[_ {:keys [eid player-item-on-cursor/place-world-item-sound]}] c]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (play place-world-item-sound)
        (swap! eid dissoc :entity/item-on-cursor)
        (world/item c
                    (item-place-position c entity)
                    (:entity/item-on-cursor entity)))))

  (entity/manual-tick [[_ {:keys [eid]}] c]
    (when (and (button-just-pressed? c :left)
               (world-item? c))
      (entity/event c eid :drop-item)))

  (entity/render-below [[_ {:keys [item]}] entity c]
    (when (world-item? c)
      (c/draw-centered c
                       (:entity/image item)
                       (item-place-position c entity))))

  (entity/draw-gui-view [[_ {:keys [eid]}] c]
    (when (not (world-item? c))
      (c/draw-centered c
                       (:entity/image (:entity/item-on-cursor @eid))
                       (c/mouse-position c))))

  (entity/clicked-inventory-cell [[_ {:keys [eid] :as data}] cell c]
    (clicked-cell data eid cell c)))
