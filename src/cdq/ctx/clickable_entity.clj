(ns cdq.ctx.clickable-entity
  (:require [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [cdq.vector2 :as v]
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
  [[:tx/toggle-inventory-visible]]) ; TODO every 'transaction' should have a sound or effect with it?

(defn- clickable->cursor [entity too-far-away?]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn clickable-entity-interaction [ctx player-entity clicked-eid]
  (if (< (v/distance (entity/position player-entity)
                     (entity/position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (on-clicked ctx clicked-eid)]
    [(clickable->cursor @clicked-eid true)  [[:tx/sound "bfxr_denied"]
                                             [:tx/show-message "Too far away"]]]))
