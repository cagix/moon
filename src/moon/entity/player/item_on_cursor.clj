(ns moon.entity.player.item-on-cursor
  (:require [gdl.graphics.gui-view :as gui-view]
            [gdl.graphics.image :as image]
            [gdl.graphics.world-view :as world-view]
            [gdl.input :refer [button-just-pressed?]]
            [gdl.math.vector :as v]
            [gdl.stage :refer [mouse-on-actor?]]
            [moon.entity :as entity]
            [moon.player :as player]
            [moon.item :refer [valid-slot? stackable?]]))

(defn- clicked-cell [eid cell]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (valid-slot? cell item-on-cursor))
     [[:tx/sound "sounds/bfxr_itemput.wav"]
      [:entity/inventory :set eid cell item-on-cursor]
      [:e/dissoc eid :entity/item-on-cursor]
      [:entity/fsm eid :dropped-item]]

     ; STACK ITEMS
     (and item-in-cell
          (stackable? item-in-cell item-on-cursor))
     [[:tx/sound "sounds/bfxr_itemput.wav"]
      [:entity/inventory :stack eid cell item-on-cursor]
      [:e/dissoc eid :entity/item-on-cursor]
      [:entity/fsm eid :dropped-item]]

     ; SWAP ITEMS
     (and item-in-cell
          (valid-slot? cell item-on-cursor))
     [[:tx/sound "sounds/bfxr_itemput.wav"]
      [:entity/inventory :remove eid cell]
      [:entity/inventory :set eid cell item-on-cursor]
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      [:e/dissoc eid :entity/item-on-cursor]
      [:entity/fsm eid :dropped-item]
      [:entity/fsm eid :pickup-item item-in-cell]])))

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
                   (world-view/mouse-position)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn- world-item? []
  (not (mouse-on-actor?)))

(defn ->v [[_ eid item]]
  {:eid eid
   :item item})

(defn pause-game? [_]
  true)

(defn manual-tick [[_ {:keys [eid]}]]
  (when (and (button-just-pressed? :left)
             (world-item?))
    [[:entity/fsm eid :drop-item]]))

(defn clicked-inventory-cell [[_ {:keys [eid]}] cell]
  (clicked-cell eid cell))

(defn enter [[_ {:keys [eid item]}]]
  [[:tx/cursor :cursors/hand-grab]
   [:e/assoc eid :entity/item-on-cursor item]])

(defn exit [[_ {:keys [eid]}]]
  ; at clicked-cell when we put it into a inventory-cell
  ; we do not want to drop it on the ground too additonally,
  ; so we dissoc it there manually. Otherwise it creates another item
  ; on the ground
  (let [entity @eid]
    (when (:entity/item-on-cursor entity)
      [[:tx/sound "sounds/bfxr_itemputground.wav"]
       [:tx/item (item-place-position entity) (:entity/item-on-cursor entity)]
       [:e/dissoc eid :entity/item-on-cursor]])))

(defn render-below [[_ {:keys [item]}] entity]
  (when (world-item?)
    (image/draw-centered (:entity/image item) (item-place-position entity))))

(defn draw-gui-view [[_ {:keys [eid]}]]
  (let [entity @eid]
    (when (and (= :player-item-on-cursor (entity/state-k entity))
               (not (world-item?)))
      (image/draw-centered (:entity/image (:entity/item-on-cursor entity))
                           (gui-view/mouse-position)))))
