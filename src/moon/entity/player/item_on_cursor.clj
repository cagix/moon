(ns ^:no-doc moon.entity.player.item-on-cursor
  (:require [gdl.input :refer [button-just-pressed?]]
            [gdl.math.vector :as v]
            [gdl.ui :as ui]
            [moon.component :refer [defc] :as component]
            [moon.graphics :as g]
            [moon.graphics.gui-view :as gui-view]
            [moon.graphics.world-view :as world-view]
            [moon.stage :refer [mouse-on-actor?]]
            [moon.item :refer [valid-slot? stackable?]]
            [moon.entity :as entity]
            [moon.world :as world]))

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
      [:tx/set-item eid cell item-on-cursor]
      [:e/dissoc eid :entity/item-on-cursor]
      [:tx/event eid :dropped-item]]

     ; STACK ITEMS
     (and item-in-cell
          (stackable? item-in-cell item-on-cursor))
     [[:tx/sound "sounds/bfxr_itemput.wav"]
      [:tx/stack-item eid cell item-on-cursor]
      [:e/dissoc eid :entity/item-on-cursor]
      [:tx/event eid :dropped-item]]

     ; SWAP ITEMS
     (and item-in-cell
          (valid-slot? cell item-on-cursor))
     [[:tx/sound "sounds/bfxr_itemput.wav"]
      [:tx/remove-item eid cell]
      [:tx/set-item eid cell item-on-cursor]
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      [:e/dissoc eid :entity/item-on-cursor]
      [:tx/event eid :dropped-item]
      [:tx/event eid :pickup-item item-in-cell]])))

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

(defc :player-item-on-cursor
  {:let {:keys [eid item]}}
  (entity/->v [[_ eid item]]
    {:eid eid
     :item item})

  (entity/pause-game? [_]
    true)

  (entity/manual-tick [_]
    (when (and (button-just-pressed? :left)
               (world-item?))
      [[:tx/event eid :drop-item]]))

  (entity/clicked-inventory-cell [_ cell]
    (clicked-cell eid cell))

  (entity/enter [_]
    [[:tx/cursor :cursors/hand-grab]
     [:e/assoc eid :entity/item-on-cursor item]])

  (entity/exit [_]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        [[:tx/sound "sounds/bfxr_itemputground.wav"]
         [:tx/item (item-place-position entity) (:entity/item-on-cursor entity)]
         [:e/dissoc eid :entity/item-on-cursor]])))

  (entity/render-below [_ entity]
    (when (world-item?)
      (g/draw-centered-image (:entity/image item) (item-place-position entity)))))

(defn- draw-item-on-cursor []
  (let [player-e* @world/player]
    (when (and (= :player-item-on-cursor (entity/state-k player-e*))
               (not (world-item?)))
      (g/draw-centered-image (:entity/image (:entity/item-on-cursor player-e*))
                             (gui-view/mouse-position)))))

(defc :widgets/draw-item-on-cursor
  (component/create [_]
    (ui/actor {:draw draw-item-on-cursor})))
