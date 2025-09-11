(ns cdq.entity.state.player-item-on-cursor
  (:require [cdq.gdx.graphics :as graphics]
            [cdq.gdx.math.vector2 :as v]))

(defn create [_eid item _ctx]
  {:item item})

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

(defn draw
  [{:keys [item]}
   entity
   {:keys [ctx/graphics
           ctx/mouseover-actor
           ctx/world-mouse-position]}]
  (when (world-item? mouseover-actor)
    [[:draw/texture-region
      (graphics/texture-region graphics (:entity/image item))
      (item-place-position world-mouse-position entity)
      {:center? true}]]))

(defn enter [{:keys [item]} eid]
  [[:tx/assoc eid :entity/item-on-cursor item]])
