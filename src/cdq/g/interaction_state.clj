(ns cdq.g.interaction-state
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.inventory :as inventory]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory]
            [cdq.vector2 :as v]
            [gdl.graphics.viewport :as viewport]
            [gdl.ui :as ui]))

(defmulti ^:private on-clicked
  (fn [ctx eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [{:keys [ctx/stage
                                               ctx/player-eid]}
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

(defn- clickable-entity-interaction [ctx player-entity clicked-eid]
  (if (< (v/distance (:position player-entity)
                     (:position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (on-clicked ctx clicked-eid)]
    [(clickable->cursor @clicked-eid true)  [[:tx/sound "bfxr_denied"]
                                             [:tx/show-message "Too far away"]]]))

(defn- mouseover-actor->cursor [actor player-entity-inventory]
  (let [inventory-slot (cdq.ui.inventory/cell-with-item? actor)]
    (cond
     (and inventory-slot
         (get-in player-entity-inventory inventory-slot)) :cursors/hand-before-grab
     (ui/window-title-bar? actor) :cursors/move-window
     (ui/button? actor) :cursors/over-button
     :else :cursors/default)))

(defn- player-effect-ctx [{:keys [ctx/mouseover-eid
                                  ctx/world-viewport]}
                          eid]
  (let [target-position (or (and mouseover-eid
                                 (:position @mouseover-eid))
                            (viewport/mouse-position world-viewport))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(extend-type cdq.g.Game
  g/InteractionState
  (interaction-state [{:keys [ctx/mouseover-eid]
                       :as ctx}
                      eid]
    (let [entity @eid
          mouseover-actor (g/mouseover-actor ctx)]
      (cond
       mouseover-actor
       [(mouseover-actor->cursor mouseover-actor (:entity/inventory entity))
        nil] ; handled by actors themself, they check player state

       (and mouseover-eid
            (:entity/clickable @mouseover-eid))
       (clickable-entity-interaction ctx entity mouseover-eid)

       :else
       (if-let [skill-id (action-bar/selected-skill (:action-bar stage))]
         (let [skill (skill-id (:entity/skills entity))
               effect-ctx (player-effect-ctx ctx eid)
               state (entity/skill-usable-state entity skill effect-ctx)]
           (if (= state :usable)
             ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
             ; different color ?
             ; => e.g. meditation no TARGET .. etc.
             [:cursors/use-skill
              [[:tx/event eid :start-action [skill effect-ctx]]]]
             ; TODO cursor as of usable state
             ; cooldown -> sanduhr kleine
             ; not-enough-mana x mit kreis?
             ; invalid-params -> depends on params ...
             [:cursors/skill-not-usable
              [[:tx/sound "bfxr_denied"]
               [:tx/show-message (case state
                                   :cooldown "Skill is still on cooldown"
                                   :not-enough-mana "Not enough mana"
                                   :invalid-params "Cannot use this here")]]]))
         [:cursors/no-skill-selected
          [[:tx/sound "bfxr_denied"]
           [:tx/show-message "No selected skill"]]])))))
