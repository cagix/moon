(ns moon.entity.player.idle
  (:require [gdl.assets :refer [play-sound]]
            [gdl.graphics.cursors :as cursors]
            [gdl.graphics.world-view :as world-view]
            [gdl.input :refer [button-just-pressed? WASD-movement-vector]]
            [gdl.math.vector :as v]
            [gdl.stage :as stage]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.effect :as effect]
            [moon.entity.inventory :as inventory]
            [moon.entity.skills :as skills]
            [moon.player :as player]
            [moon.widgets.action-bar :as action-bar]
            [moon.widgets.inventory :as widgets.inventory]
            [moon.widgets.player-message :as player-message]
            [moon.world.mouseover :as mouseover]))

(defn- denied [text]
  (play-sound "sounds/bfxr_denied.wav")
  (player-message/show text))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (a/visible? (widgets.inventory/window))
     (do
      (play-sound "sounds/bfxr_takeit.wav")
      (swap! eid assoc :entity/destroyed? true)
      [[:entity/fsm player/eid :pickup-item item]])

     (inventory/can-pickup-item? @player/eid item)
     (do
      (play-sound "sounds/bfxr_pickup.wav")
      (swap! eid assoc :entity/destroyed? true)
      [[:entity/inventory :pickup player/eid item]])

     :else
     (do
      (play-sound "sounds/bfxr_denied.wav")
      (player-message/show "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (a/toggle-visible! (widgets.inventory/window)))

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

(defn- inventory-cell-with-item? [actor]
  (and (a/parent actor)
       (= "inventory-cell" (a/name (a/parent actor)))
       (get-in (:entity/inventory @player/eid)
               (a/id (a/parent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (stage/mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)      :cursors/move-window
     (ui/button? actor)                :cursors/over-button
     :else                             :cursors/default)))

(defn- effect-ctx [eid]
  (let [target-position (or (and mouseover/eid (:position @mouseover/eid))
                            (world-view/mouse-position))]
    {:effect/source eid
     :effect/target mouseover/eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor?)
     [(mouseover-actor->cursor) (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover/eid (:entity/clickable @mouseover/eid))
     (clickable-entity-interaction entity mouseover/eid)

     :else
     (if-let [skill-id (action-bar/selected-skill)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (effect-ctx eid)
             state (effect/with-ctx effect-ctx
                     (skills/usable-state entity skill))]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn []
               [[:entity/fsm eid :start-action [skill effect-ctx]]])])
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

(defn ->v [eid]
  {:eid eid})

(defn pause-game? [_]
  true)

(defn manual-tick [{:keys [eid]}]
  (if-let [movement-vector (WASD-movement-vector)]
    [[:entity/fsm eid :movement-input movement-vector]]
    (let [[cursor on-click] (interaction-state eid)]
      (cursors/set cursor)
      (when (button-just-pressed? :left)
        (on-click)))))

(defn clicked-inventory-cell [{:keys [eid]} cell]
  ; TODO no else case
  (when-let [item (get-in (:entity/inventory @eid) cell)]
    (play-sound "sounds/bfxr_takeit.wav")
    [[:entity/fsm eid :pickup-item item]
     [:entity/inventory :remove eid cell]]))

(defn clicked-skillmenu-skill [{:keys [eid]} skill]
  (let [free-skill-points (:entity/free-skill-points @eid)]
    ; TODO no else case, no visible free-skill-points
    (when (and (pos? free-skill-points)
               (not (skills/has-skill? @eid skill)))
      (swap! eid assoc :entity/free-skill-points (dec free-skill-points))
      [[:entity/skills eid :add skill]])))
