(ns cdq.entity.state.player-idle
  (:require [cdq.ui.action-bar :as action-bar] ; 'ctx/render-frame'
            [cdq.controls :as controls]        ; input
            [cdq.gdx.math.vector2 :as v]
            [cdq.inventory :as inventory] ; entity.inventory
            [cdq.skill :as skill]
            [cdq.entity :as entity]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]))

(defn- action-bar-selected-skill [stage]
  (-> stage
      :action-bar
      action-bar/selected-skill))

(defn distance [a b]
  (v/distance (entity/position a)
              (entity/position b)))

(defn in-click-range? [player-entity clicked-entity]
  (< (distance player-entity clicked-entity)
     (:entity/click-distance-tiles player-entity)))

(defn- player-effect-ctx [mouseover-eid world-mouse-position player-eid]
  (let [target-position (or (and mouseover-eid
                                 (entity/position @mouseover-eid))
                            world-mouse-position)]
    {:effect/source player-eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (entity/position @player-eid) target-position)}))

; TODO this is just mouseover state!
; either over an ui-actor
; or clickable-entity
; otherwise nothing mouseover (or a target) or a tile ?
; or nothing ?
; thats it ?
; it depends only on graphics layers
; * black
; * tiled-map -> what tile ... can click also
; * entities -> check , target otherwise
; * user interface -> check
(defn interaction-state
  [{:keys [ctx/mouseover-actor
           ctx/mouseover-eid
           ctx/stage
           ctx/world-mouse-position]}
   player-eid]
  (cond
   mouseover-actor
   [:interaction-state/mouseover-actor mouseover-actor]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (in-click-range? @player-eid @mouseover-eid)}]

   :else
   (if-let [skill-id (action-bar-selected-skill stage)]
     (let [entity @player-eid
           skill (skill-id (:entity/skills entity))
           effect-ctx (player-effect-ctx mouseover-eid world-mouse-position player-eid)
           state (skill/usable-state entity skill effect-ctx)]
       (if (= state :usable)
         [:interaction-state.skill/usable [skill effect-ctx]]
         [:interaction-state.skill/not-usable state]))
     [:interaction-state/no-skill-selected])))

(defn inventory-window-visible? [stage]
  (-> stage :windows :inventory-window actor/visible?))

(defn can-pickup-item? [entity item]
  (inventory/can-pickup-item? (:entity/inventory entity) item))

(defn interaction-state->txs [ctx player-eid]
  (let [[k params] (interaction-state ctx player-eid)]
    (case k
      :interaction-state/mouseover-actor nil ; handled by ui actors themself.

      :interaction-state/clickable-mouseover-eid
      (let [{:keys [clicked-eid
                    in-click-range?]} params]
        (if in-click-range?
          (case (:type (:entity/clickable @clicked-eid))
            :clickable/player
            [[:tx/toggle-inventory-visible]]

            :clickable/item
            (let [item (:entity/item @clicked-eid)]
              (cond
               (inventory-window-visible? (:ctx/stage ctx))
               [[:tx/sound "bfxr_takeit"]
                [:tx/mark-destroyed clicked-eid]
                [:tx/event player-eid :pickup-item item]]

               (can-pickup-item? @player-eid item)
               [[:tx/sound "bfxr_pickup"]
                [:tx/mark-destroyed clicked-eid]
                [:tx/pickup-item player-eid item]]

               :else
               [[:tx/sound "bfxr_denied"]
                [:tx/show-message "Your Inventory is full"]])))
          [[:tx/sound "bfxr_denied"]
           [:tx/show-message "Too far away"]]))

      :interaction-state.skill/usable
      (let [[skill effect-ctx] params]
        [[:tx/event player-eid :start-action [skill effect-ctx]]])

      :interaction-state.skill/not-usable
      (let [state params]
        [[:tx/sound "bfxr_denied"]
         [:tx/show-message (case state
                             :cooldown "Skill is still on cooldown"
                             :not-enough-mana "Not enough mana"
                             :invalid-params "Cannot use this here")]])

      :interaction-state/no-skill-selected
      [[:tx/sound "bfxr_denied"]
       [:tx/show-message "No selected skill"]])))
