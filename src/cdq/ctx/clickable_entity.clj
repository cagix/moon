(ns cdq.ctx.clickable-entity
  (:require [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [gdl.math.vector2 :as v]
            [gdl.ui :as ui]))

(defmulti on-clicked
  (fn [ctx eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [{:keys [ctx/player-eid
                                               ctx/stage]}
                                       eid]
  (let [item (:entity/item @eid)]
    (cond
     (-> stage :windows :inventory-window ui/visible?)
     [[:tx/sound "bfxr_takeit"]
      [:tx/mark-destroyed eid]
      [:tx/event player-eid :pickup-item item]]

     (inventory/can-pickup-item? (:entity/inventory @player-eid) item)
     [[:tx/sound "bfxr_pickup"]
      [:tx/mark-destroyed eid]
      [:tx/pickup-item player-eid item]]

     :else
     [[:tx/sound "bfxr_denied"]
      [:tx/show-message "Your Inventory is full"]])))

(defmethod on-clicked :clickable/player [_ctx _eid]
  [[:tx/toggle-inventory-visible]])

(defn- clickable->cursor [entity & {:keys [too-far-away?]}]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn distance [a b]
  (v/distance (entity/position a)
              (entity/position b)))

(defn in-click-range? [player-entity clicked-entity]
  (< (distance player-entity clicked-entity)
     (:entity/click-distance-tiles player-entity)))

(defn clickable-entity-interaction [ctx player-entity clicked-eid]
  (if (in-click-range? player-entity @clicked-eid)
    [(clickable->cursor @clicked-eid :too-far-away? false)
     (on-clicked ctx clicked-eid)]
    [(clickable->cursor @clicked-eid :too-far-away? true)
     [[:tx/sound "bfxr_denied"]
      [:tx/show-message "Too far away"]]]))
