(ns forge.entity.state.player-idle
  (:require [anvil.app :refer [play-sound]]
            [anvil.entity :refer [send-event]]
            [anvil.graphics :refer [set-cursor world-mouse-position]]
            [anvil.ui :refer [window-title-bar? button?]]
            [anvil.world :refer [player-eid mouseover-eid]]
            [clojure.gdx.input :refer [button-just-pressed?]]
            [clojure.gdx.math.vector2 :as v]
            [clojure.gdx.scene2d.actor :as actor]
            [forge.controls :as controls]
            [forge.entity.inventory :refer [can-pickup-item? pickup-item remove-item]]
            [forge.entity.skills :refer [has-skill? add-skill]]
            [forge.screens.stage :refer [mouse-on-actor?]]
            [forge.skill :as skill]
            [forge.ui.action-bar :as action-bar]
            [forge.ui.inventory :as inventory]
            [forge.ui.player-message :as player-message]))

(defn- denied [text]
  (play-sound "bfxr_denied")
  (player-message/show text))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (actor/visible? (inventory/window))
     (do
      (play-sound "bfxr_takeit")
      (swap! eid assoc :entity/destroyed? true)
      (send-event player-eid :pickup-item item))

     (can-pickup-item? @player-eid item)
     (do
      (play-sound "bfxr_pickup")
      (swap! eid assoc :entity/destroyed? true)
      (pickup-item player-eid item))

     :else
     (do
      (play-sound "bfxr_denied")
      (player-message/show "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (actor/toggle-visible! (inventory/window)))

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
       (get-in (:entity/inventory @player-eid)
               (actor/user-object (.getParent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (window-title-bar? actor)      :cursors/move-window
     (button? actor)                :cursors/over-button
     :else                             :cursors/default)))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and mouseover-eid (:position @mouseover-eid))
                            (world-mouse-position))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (mouse-on-actor?)
     [(mouseover-actor->cursor) (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction entity mouseover-eid)

     :else
     (if-let [skill-id (action-bar/selected-skill)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx eid)
             state (skill/usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn [] (send-event eid :start-action [skill effect-ctx]))])
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

(defn ->v [[_ eid]]
  {:eid eid})

(defn pause-game? [_]
  true)

(defn manual-tick [[_ {:keys [eid]}]]
  (if-let [movement-vector (controls/movement-vector)]
    (send-event eid :movement-input movement-vector)
    (let [[cursor on-click] (interaction-state eid)]
      (set-cursor cursor)
      (when (button-just-pressed? :left)
        (on-click)))))

(defn clicked-inventory-cell [[_ {:keys [eid]}] cell]
  ; TODO no else case
  (when-let [item (get-in (:entity/inventory @eid) cell)]
    (play-sound "bfxr_takeit")
    (send-event eid :pickup-item item)
    (remove-item eid cell)))

(defn clicked-skillmenu-skill [[_ {:keys [eid]}] skill]
  (let [free-skill-points (:entity/free-skill-points @eid)]
    ; TODO no else case, no visible free-skill-points
    (when (and (pos? free-skill-points)
               (not (has-skill? @eid skill)))
      (swap! eid assoc :entity/free-skill-points (dec free-skill-points))
      (swap! eid add-skill skill))))
