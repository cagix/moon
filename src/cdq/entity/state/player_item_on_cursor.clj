(ns cdq.entity.state.player-item-on-cursor
  (:require [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [cdq.state :as state]
            [cdq.utils :refer [defcomponent]]
            [cdq.vector2 :as v]
            [gdl.input :as input]
            [gdl.graphics.viewport :as viewport]
            [gdl.ui :as ui]))

(defn- clicked-cell [eid cell]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (inventory/valid-slot? cell item-on-cursor))
     [[:tx/sound "bfxr_itemput"]
      [:tx/dissoc eid :entity/item-on-cursor]
      [:tx/set-item eid cell item-on-cursor]
      [:tx/event eid :dropped-item]]

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     [[:tx/sound "bfxr_itemput"]
      [:tx/dissoc eid :entity/item-on-cursor]
      [:tx/stack-item eid cell item-on-cursor]
      [:tx/event eid :dropped-item]]

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     [[:tx/sound "bfxr_itemput"]
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      [:tx/dissoc eid :entity/item-on-cursor]
      [:tx/remove-item eid cell]
      [:tx/set-item eid cell item-on-cursor]
      [:tx/event eid :dropped-item]
      [:tx/event eid :pickup-item item-in-cell]])))

(defn- world-item? [{:keys [ctx/stage
                            ctx/ui-viewport]}]
  (not (ui/hit stage (viewport/mouse-position ui-viewport))))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn- item-place-position [{:keys [ctx/world-viewport]} entity]
  (placement-point (:position entity)
                   (viewport/mouse-position world-viewport)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defcomponent :player-item-on-cursor
  (entity/create [[_ eid item] _ctx]
    {:item item})

  (entity/render-below! [[_ {:keys [item]}] entity ctx]
    (when (world-item? ctx)
      (draw/centered ctx
                     (:entity/image item)
                     (item-place-position ctx entity))))

  (state/cursor [_] :cursors/hand-grab)

  (state/pause-game? [_] true)

  (state/enter! [[_ {:keys [item]}] eid]
    [[:tx/assoc eid :entity/item-on-cursor item]])

  (state/exit! [_ eid ctx]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        [[:tx/sound "bfxr_itemputground"]
         [:tx/dissoc eid :entity/item-on-cursor]
         [:tx/spawn-item (item-place-position ctx entity) (:entity/item-on-cursor entity)]])))

  (state/manual-tick [_ eid ctx]
    (when (and (input/button-just-pressed? :left)
               (world-item? ctx))
      [[:tx/event eid :drop-item]]))

  (state/clicked-inventory-cell [_ eid cell]
    (clicked-cell eid cell))

  (state/draw-gui-view [_ eid {:keys [ctx/ui-viewport] :as ctx}]
    (when (not (world-item? ctx))
      (draw/centered ctx
                     (:entity/image (:entity/item-on-cursor @eid))
                     (viewport/mouse-position ui-viewport)))))
