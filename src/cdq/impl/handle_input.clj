(ns cdq.impl.handle-input
  (:require [cdq.entity :as entity :refer [manual-tick]]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.skill :as skill]
            [cdq.tx :as tx]
            [cdq.math.vector2 :as v]
            [cdq.ui :as ui]
            [cdq.ui.actor :as actor]
            [cdq.ui.stage :as stage]
            [cdq.widgets.inventory :as widgets.inventory]
            [cdq.world :refer [world-item?
                               get-inventory
                               selected-skill
                               player-movement-vector]]))

(defmulti ^:private on-clicked
  (fn [eid c]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid {:keys [cdq.context/player-eid] :as c}]
  (let [item (:entity/item @eid)]
    (cond
     (actor/visible? (get-inventory c))
     (do
      (tx/sound c "bfxr_takeit")
      (tx/mark-destroyed eid)
      (tx/event c player-eid :pickup-item item))

     (entity/can-pickup-item? @player-eid item)
     (do
      (tx/sound c "bfxr_pickup")
      (tx/mark-destroyed eid)
      (widgets.inventory/pickup-item c player-eid item))

     :else
     (do
      (tx/sound c "bfxr_denied")
      (tx/show-player-msg c "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_ c]
  (tx/toggle-inventory-window c))

(defn- clickable->cursor [entity too-far-away?]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn- clickable-entity-interaction [c player-entity clicked-eid]
  (if (< (v/distance (:position player-entity)
                     (:position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (fn []
                                              (on-clicked clicked-eid c))]
    [(clickable->cursor @clicked-eid true)  (fn []
                                              (tx/sound c "bfxr_denied")
                                              (tx/show-player-msg c "Too far away"))]))

(defn- inventory-cell-with-item? [{:keys [cdq.context/player-eid] :as c} actor]
  (and (actor/parent actor)
       (= "inventory-cell" (actor/name (actor/parent actor)))
       (get-in (:entity/inventory @player-eid)
               (actor/user-object (actor/parent actor)))))

(defn- mouseover-actor->cursor [{:keys [cdq.context/stage] :as c}]
  (let [actor (stage/mouse-on-actor? stage)]
    (cond
     (inventory-cell-with-item? c actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)        :cursors/move-window
     (ui/button? actor)                  :cursors/over-button
     :else                               :cursors/default)))

(defn- player-effect-ctx [{:keys [cdq.context/mouseover-eid
                                  cdq.graphics/world-viewport]} eid]
  (let [target-position (or (and mouseover-eid
                                 (:position @mouseover-eid))
                            (graphics/world-mouse-position world-viewport))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [{:keys [cdq.context/mouseover-eid
                                  cdq.context/stage] :as c} eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor? stage)
     [(mouseover-actor->cursor c)
      (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction c entity mouseover-eid)

     :else
     (if-let [skill-id (selected-skill c)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx c eid)
             state (skill/usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn []
               (tx/event c eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (tx/sound c "bfxr_denied")
               (tx/show-player-msg c (case state
                                       :cooldown "Skill is still on cooldown"
                                       :not-enough-mana "Not enough mana"
                                       :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn []
          (tx/sound c "bfxr_denied")
          (tx/show-player-msg c "No selected skill"))]))))

(defmethod manual-tick :player-idle [[_ {:keys [eid]}] c]
  (if-let [movement-vector (player-movement-vector)]
    (tx/event c eid :movement-input movement-vector)
    (let [[cursor on-click] (interaction-state c eid)]
      (tx/cursor c cursor)
      (when (input/button-just-pressed? :left)
        (on-click)))))

(defmethod manual-tick :player-item-on-cursor [[_ {:keys [eid]}] c]
  (when (and (input/button-just-pressed? :left)
             (world-item? c))
    (tx/event c eid :drop-item)))
