(ns ^:no-doc core.entity.state.player-item-on-cursor
  (:require [core.math.vector :as v]
            [core.component :as component :refer [defcomponent]]
            [core.entity :as entity]
            [core.entity.inventory :as inventory]
            [core.entity.player :as player]
            [core.graphics :as g]
            [core.graphics.views :refer [world-mouse-position gui-mouse-position]]
            [core.entity.state :as state]
            [core.screens.stage :as stage])
  (:import (com.badlogic.gdx Gdx Input$Buttons)))

(defn- clicked-cell [{:keys [entity/id] :as entity*} cell]
  (let [inventory (:entity/inventory entity*)
        item (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity*)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item)
          (inventory/valid-slot? cell item-on-cursor))
     [[:tx/sound "sounds/bfxr_itemput.wav"]
      [:tx/set-item id cell item-on-cursor]
      [:e/dissoc id :entity/item-on-cursor]
      [:tx/event id :dropped-item]]

     ; STACK ITEMS
     (and item (inventory/stackable? item item-on-cursor))
     [[:tx/sound "sounds/bfxr_itemput.wav"]
      [:tx/stack-item id cell item-on-cursor]
      [:e/dissoc id :entity/item-on-cursor]
      [:tx/event id :dropped-item]]

     ; SWAP ITEMS
     (and item
          (inventory/valid-slot? cell item-on-cursor))
     [[:tx/sound "sounds/bfxr_itemput.wav"]
      [:tx/remove-item id cell]
      [:tx/set-item id cell item-on-cursor]
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      [:e/dissoc id :entity/item-on-cursor]
      [:tx/event id :dropped-item]
      [:tx/event id :pickup-item item]])))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.

(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn- item-place-position [ctx entity*]
  (placement-point (:position entity*)
                   (world-mouse-position ctx)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity*) 0.1)))

(defn- world-item? [ctx]
  (not (stage/mouse-on-actor? ctx)))

(defcomponent :player-item-on-cursor
  {:let {:keys [eid item]}}
  (component/create [[_ eid item] _ctx]
    {:eid eid
     :item item})

  (state/pause-game? [_]
    true)

  (state/manual-tick [_ ctx]
    (when (and (.isButtonJustPressed Gdx/input Input$Buttons/LEFT)
               (world-item? ctx))
      [[:tx/event eid :drop-item]]))

  (state/clicked-inventory-cell [_ cell]
    (clicked-cell @eid cell))

  (state/enter [_ _ctx]
    [[:tx/cursor :cursors/hand-grab]
     [:e/assoc eid :entity/item-on-cursor item]])

  (state/exit [_ ctx]
    ; at context.ui.inventory-window/clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity* @eid]
      (when (:entity/item-on-cursor entity*)
        [[:tx/sound "sounds/bfxr_itemputground.wav"]
         [:tx/item (item-place-position ctx entity*) (:entity/item-on-cursor entity*)]
         [:e/dissoc eid :entity/item-on-cursor]])))

  (entity/render-below [_ entity* g ctx]
    (when (world-item? ctx)
      (g/draw-centered-image g (:entity/image item) (item-place-position ctx entity*)))))

(defn draw-item-on-cursor [g ctx]
  (let [player-entity* (player/entity* ctx)]
    (when (and (= :player-item-on-cursor (entity/state player-entity*))
               (not (world-item? ctx)))
      (g/draw-centered-image g
                             (:entity/image (:entity/item-on-cursor player-entity*))
                             (gui-mouse-position ctx)))))
