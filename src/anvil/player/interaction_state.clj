(ns anvil.player.interaction-state
  (:require [anvil.entity :as entity]
            [anvil.skill :as skill]
            [anvil.player :as player]
            [anvil.world :as world]
            [gdl.assets :refer [play-sound]]
            [gdl.graphics :as g]
            [gdl.math.vector :as v]
            [gdl.stage :as stage]
            [gdl.ui :refer [window-title-bar? button?]]
            [gdl.ui.actor :as actor]))

(defn- denied [text]
  (play-sound "bfxr_denied")
  (stage/show-player-msg text))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (actor/visible? (stage/get-inventory))
     (do
      (play-sound "bfxr_takeit")
      (swap! eid assoc :entity/destroyed? true)
      (entity/event world/player-eid :pickup-item item))

     (entity/can-pickup-item? @world/player-eid item)
     (do
      (play-sound "bfxr_pickup")
      (swap! eid assoc :entity/destroyed? true)
      (entity/pickup-item world/player-eid item))

     :else
     (do
      (play-sound "bfxr_denied")
      (stage/show-player-msg "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (actor/toggle-visible! (stage/get-inventory)))

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
    [(clickable->cursor @clicked-eid false) (fn [] (on-clicked clicked-eid))]
    [(clickable->cursor @clicked-eid true)  (fn [] (denied "Too far away"))]))

(defn- inventory-cell-with-item? [^com.badlogic.gdx.scenes.scene2d.Actor actor]
  (and (.getParent actor)
       (= "inventory-cell" (.getName (.getParent actor)))
       (get-in (:entity/inventory @world/player-eid)
               (actor/user-object (.getParent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (stage/mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (window-title-bar? actor)      :cursors/move-window
     (button? actor)                :cursors/over-button
     :else                             :cursors/default)))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and world/mouseover-eid
                                 (:position @world/mouseover-eid))
                            (g/world-mouse-position))]
    {:effect/source eid
     :effect/target world/mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor?)
     [(mouseover-actor->cursor) (fn [] nil)] ; handled by actors themself, they check player state

     (and world/mouseover-eid
          (:entity/clickable @world/mouseover-eid))
     (clickable-entity-interaction entity world/mouseover-eid)

     :else
     (if-let [skill-id (stage/selected-skill)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx eid)
             state (skill/usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn [] (entity/event eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (denied (case state
                         :cooldown "Skill is still on cooldown"
                         :not-enough-mana "Not enough mana"
                         :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn [] (denied "No selected skill"))]))))

(defn-impl player/interaction-state [eid]
  (interaction-state eid))
