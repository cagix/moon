(ns moon.entity.player.idle
  (:require [gdl.input :refer [button-just-pressed? WASD-movement-vector]]
            [gdl.math.vector :as v]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.effect :as effect]
            [moon.entity :as entity]
            [moon.entity.inventory :as inventory]
            [moon.entity.player :as player]
            [moon.entity.skills :as skills]
            [moon.graphics.world-view :as world-view]
            [moon.stage :as stage]
            [moon.widgets.action-bar :as action-bar]
            [moon.widgets.windows :as windows]
            [moon.world.mouseover :as mouseover]))

(defn- denied [text]
  [[:tx/sound "sounds/bfxr_denied.wav"]
   [:tx/msg-to-player text]])

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (a/visible? (windows/inventory))
     [[:tx/sound "sounds/bfxr_takeit.wav"]
      [:e/destroy eid]
      [:entity/fsm player/eid :pickup-item item]]

     (inventory/can-pickup-item? player/eid item)
     [[:tx/sound "sounds/bfxr_pickup.wav"]
      [:e/destroy eid]
      [:tx/pickup-item player/eid item]]

     :else
     [[:tx/sound "sounds/bfxr_denied.wav"]
      [:tx/msg-to-player "Your Inventory is full"]])))

(defmethod on-clicked :clickable/player [_]
  (a/toggle-visible! (windows/inventory))) ; TODO no tx

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
                     (effect/skill-usable-state entity skill))]
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

(defc :player-idle
  {:let {:keys [eid]}}
  (entity/->v [[_ eid]]
    {:eid eid})

  (entity/pause-game? [_]
    true)

  (entity/manual-tick [_]
    (if-let [movement-vector (WASD-movement-vector)]
      [[:entity/fsm eid :movement-input movement-vector]]
      (let [[cursor on-click] (interaction-state eid)]
        (cons [:tx/cursor cursor]
              (when (button-just-pressed? :left)
                (on-click))))))

  (entity/clicked-inventory-cell [_ cell]
    ; TODO no else case
    (when-let [item (get-in (:entity/inventory @eid) cell)]
      [[:tx/sound "sounds/bfxr_takeit.wav"]
       [:entity/fsm eid :pickup-item item]
       [:tx/remove-item eid cell]]))

  (entity/clicked-skillmenu-skill [_ skill]
    (let [free-skill-points (:entity/free-skill-points @eid)]
      ; TODO no else case, no visible free-skill-points
      (when (and (pos? free-skill-points)
                 (not (skills/has-skill? @eid skill)))
        [[:e/assoc eid :entity/free-skill-points (dec free-skill-points)]
         [:tx/add-skill eid skill]]))))
