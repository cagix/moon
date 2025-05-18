(ns cdq.entity.state.player-idle
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [cdq.input :as input]
            [cdq.state :as state]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory]
            [cdq.utils :refer [defcomponent]]
            [cdq.vector2 :as v]
            [gdl.graphics.viewport :as viewport]
            [gdl.input]
            [gdl.ui :as ui]))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (-> ctx/stage :windows :inventory-window ui/visible?)
     [[:tx/sound "bfxr_takeit"]
      [:tx/mark-destroyed eid]
      [:tx/event ctx/player-eid :pickup-item item]]

     (inventory/can-pickup-item? (:entity/inventory @ctx/player-eid) item)
     [[:tx/sound "bfxr_pickup"]
      [:tx/mark-destroyed eid]
      [:tx/pickup-item ctx/player-eid item]]

     :else
     [[:tx/sound "bfxr_denied"]
      [:tx/show-message "Your Inventory is full"]])))

(defmethod on-clicked :clickable/player [_]
  [[:tx/toggle-inventory-visible]]) ; TODO every 'transaction' should have a sound or effect with it?

(defn- clickable->cursor [entity too-far-away?]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn- clickable-entity-interaction [player-entity clicked-eid]
  (if (< (v/distance (:position player-entity)
                     (:position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (on-clicked clicked-eid)]
    [(clickable->cursor @clicked-eid true)  [[:tx/sound "bfxr_denied"]
                                             [:tx/show-message "Too far away"]]]))

(defn- mouseover-actor->cursor [actor]
  (cond
   (cdq.ui.inventory/cell-with-item? actor) :cursors/hand-before-grab
   (ui/window-title-bar? actor) :cursors/move-window
   (ui/button? actor) :cursors/over-button
   :else :cursors/default))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and ctx/mouseover-eid
                                 (:position @ctx/mouseover-eid))
                            (viewport/mouse-position ctx/world-viewport))]
    {:effect/source eid
     :effect/target ctx/mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid
        mouseover-actor (ui/hit ctx/stage (viewport/mouse-position ctx/ui-viewport))]
    (cond
     mouseover-actor
     [(mouseover-actor->cursor mouseover-actor)
      nil] ; handled by actors themself, they check player state

     (and ctx/mouseover-eid
          (:entity/clickable @ctx/mouseover-eid))
     (clickable-entity-interaction entity ctx/mouseover-eid)

     :else
     (if-let [skill-id (action-bar/selected-skill (:action-bar ctx/stage))]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx eid)
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
         [:tx/show-message "No selected skill"]]]))))

(defcomponent :player-idle
  (entity/create [[_ eid]]
    {:eid eid})

  (state/pause-game? [_] true)

  (state/manual-tick [[_ {:keys [eid]}]]
    (if-let [movement-vector (input/player-movement-vector)]
      [[:tx/event eid :movement-input movement-vector]]
      (let [[cursor on-click] (interaction-state eid)]
        (cons [:tx/set-cursor cursor]
              (when (gdl.input/button-just-pressed? :left)
                on-click)))))

  (state/clicked-inventory-cell [[_ {:keys [eid]}] cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      [[:tx/sound "bfxr_takeit"]
       [:tx/event eid :pickup-item item]
       [:tx/remove-item eid cell]])))
