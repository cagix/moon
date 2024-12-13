(ns anvil.entity.state.player-item-on-cursor
  (:require [anvil.component :as component]
            [anvil.entity.fsm :as fsm]
            [anvil.world :as world]
            [gdl.assets :refer [play-sound]]
            [gdl.graphics :as g]
            [gdl.input :refer [button-just-pressed?]]
            [gdl.stage :refer [mouse-on-actor?]]
            [gdl.math.vector :as v]
            [gdl.utils :refer [defmethods]]))

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
      (fsm/event eid :drop-item)))

  (component/render-below [[_ {:keys [item]}] entity]
    (when (world-item?)
      (g/draw-centered (:entity/image item)
                       (item-place-position entity))))

  (component/draw-gui-view [[_ {:keys [eid]}]]
    (when (not (world-item?))
      (g/draw-centered (:entity/image (:entity/item-on-cursor @eid))
                       (g/mouse-position)))))
