(ns anvil.player
  (:require [anvil.entity :as entity]
            [anvil.skill :as skill]
            [cdq.context :as w]
            [gdl.context :as c :refer [play-sound]]
            [gdl.math.vector :as v]
            [gdl.ui :refer [window-title-bar? button?]]
            [clojure.gdx.scene2d.actor :as actor]))

(defmulti ^:private on-clicked
  (fn [eid c]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid {:keys [cdq.context/player-eid] :as c}]
  (let [item (:entity/item @eid)]
    (cond
     (actor/visible? (w/get-inventory c))
     (do
      (play-sound c "bfxr_takeit")
      (swap! eid assoc :entity/destroyed? true)
      (entity/event c player-eid :pickup-item item))

     (entity/can-pickup-item? @player-eid item)
     (do
      (play-sound c "bfxr_pickup")
      (swap! eid assoc :entity/destroyed? true)
      (entity/pickup-item c player-eid item))

     :else
     (do
      (play-sound c "bfxr_denied")
      (w/show-player-msg c "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_ c]
  (actor/toggle-visible! (w/get-inventory c)))

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
                                              (play-sound c "bfx_denied")
                                              (w/show-player-msg c "Too far away"))]))

(defn- inventory-cell-with-item? [{:keys [cdq.context/player-eid] :as c} actor]
  (and (actor/parent actor)
       (= "inventory-cell" (.getName (actor/parent actor)))
       (get-in (:entity/inventory @player-eid)
               (actor/user-object (actor/parent actor)))))

(defn- mouseover-actor->cursor [c]
  (let [actor (c/mouse-on-actor? c)]
    (cond
     (inventory-cell-with-item? c actor) :cursors/hand-before-grab
     (window-title-bar? actor)           :cursors/move-window
     (button? actor)                     :cursors/over-button
     :else                               :cursors/default)))

(defn- player-effect-ctx [{:keys [cdq.context/mouseover-eid] :as c} eid]
  (let [target-position (or (and mouseover-eid
                                 (:position @mouseover-eid))
                            (c/world-mouse-position c))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn interaction-state [{:keys [cdq.context/mouseover-eid] :as c} eid]
  (let [entity @eid]
    (cond
     (c/mouse-on-actor? c)
     [(mouseover-actor->cursor c)
      (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction c entity mouseover-eid)

     :else
     (if-let [skill-id (w/selected-skill c)]
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
               (entity/event c eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (play-sound c "bfxr_denied")
               (w/show-player-msg c (case state
                                      :cooldown "Skill is still on cooldown"
                                      :not-enough-mana "Not enough mana"
                                      :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn []
          (play-sound c "bfxr_denied")
          (w/show-player-msg c "No selected skill"))]))))
