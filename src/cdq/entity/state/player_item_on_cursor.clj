(ns cdq.entity.state.player-item-on-cursor
  (:require [cdq.inventory :as inventory]
            [cdq.graphics :as graphics]
            [clojure.math.vector2 :as v]
            [clojure.input :as input]))

(defn world-item? [mouseover-actor]
  (not mouseover-actor))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn item-place-position [world-mouse-position entity]
  (placement-point (:body/position (:entity/body entity))
                   world-mouse-position
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn handle-input [eid {:keys [ctx/input ctx/mouseover-actor]}]
  (when (and (input/button-just-pressed? input :left)
             (world-item? mouseover-actor))
    [[:tx/event eid :drop-item]]))

(defn clicked-inventory-cell [eid cell]
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

(defn draw-gui-view
  [eid
   {:keys [ctx/graphics
           ctx/mouseover-actor
           ctx/ui-mouse-position]}]
  (when (not (world-item? mouseover-actor))
    [[:draw/texture-region
      (graphics/texture-region graphics (:entity/image (:entity/item-on-cursor @eid)))
      ui-mouse-position
      {:center? true}]]))
