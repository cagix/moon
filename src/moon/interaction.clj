(ns moon.interaction
  (:require [gdl.math.vector :as v]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.effect :as effect]
            [moon.entity :as entity]
            [moon.graphics :as g]
            [moon.stage :as stage]
            [moon.world :as world :refer [mouseover-eid]]))

(defn- denied [text]
  [[:tx/sound "sounds/bfxr_denied.wav"]
   [:tx/msg-to-player text]])

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (a/visible? (world/get-window :inventory-window))
     [[:tx/sound "sounds/bfxr_takeit.wav"]
      [:e/destroy eid]
      [:tx/event world/player :pickup-item item]]

     (entity/can-pickup-item? world/player item)
     [[:tx/sound "sounds/bfxr_pickup.wav"]
      [:e/destroy eid]
      [:tx/pickup-item world/player item]]

     :else
     [[:tx/sound "sounds/bfxr_denied.wav"]
      [:tx/msg-to-player "Your Inventory is full"]])))

(defmethod on-clicked :clickable/player [_]
  (a/toggle-visible! (world/get-window :inventory-window))) ; TODO no tx

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
       (get-in (:entity/inventory @world/player)
               (a/id (a/parent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (stage/mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor) :cursors/move-window
     (ui/button? actor) :cursors/over-button
     :else :cursors/default)))

(defn- effect-ctx [eid]
  (let [target-position (or (and mouseover-eid (:position @mouseover-eid))
                            (g/world-mouse-position))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor?)
     [(mouseover-actor->cursor) (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction entity mouseover-eid)

     :else
     (if-let [skill-id (entity/selected-skill)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (effect-ctx eid)
             state (effect/with-ctx effect-ctx
                     (entity/skill-usable-state entity skill))]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn []
               [[:tx/event eid :start-action [skill effect-ctx]]])])
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

(.bindRoot #'entity/interaction-state interaction-state)
