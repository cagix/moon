(ns cdq.ctx.interaction-state
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.windows.inventory :as inventory-window]
            [gdl.c :as c]
            [gdl.math.vector2 :as v]
            [gdl.ui.actor :as actor]))

(defn inventory-window-visible? [stage]
  (-> stage :windows :inventory-window actor/visible?))

(defn selected-skill [stage]
  (action-bar/selected-skill (:action-bar stage)))

(defn can-pickup-item? [entity item]
  (inventory/can-pickup-item? (:entity/inventory entity) item))

(defn distance [a b]
  (v/distance (entity/position a)
              (entity/position b)))

(defn in-click-range? [player-entity clicked-entity]
  (< (distance player-entity clicked-entity)
     (:entity/click-distance-tiles player-entity)))

(defn interaction-state
  [{:keys [ctx/stage
           ctx/mouseover-eid] :as ctx}
   player-eid]
  (let [mouseover-actor (c/mouseover-actor ctx)]
    (cond
     mouseover-actor
     [:interaction-state/mouseover-actor mouseover-actor]

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     [:interaction-state/clickable-mouseover-eid
      {:clicked-eid mouseover-eid
       :in-click-range? (in-click-range? @player-eid @mouseover-eid)}]

     :else
     (if-let [skill-id (selected-skill stage)]
       (let [entity @player-eid
             skill (skill-id (:entity/skills entity))
             effect-ctx (ctx/player-effect-ctx ctx player-eid)
             state (entity/skill-usable-state entity skill effect-ctx)]
         (if (= state :usable)
           [:interaction-state.skill/usable [skill effect-ctx]]
           [:interaction-state.skill/not-usable state]))
       [:interaction-state/no-skill-selected]))))

(defn ->cursor [player-eid ctx]
  (let [[k params] (interaction-state ctx player-eid)]
    (case k
      :interaction-state/mouseover-actor
      (let [actor params]
        (let [inventory-slot (inventory-window/cell-with-item? actor)]
          (cond
           (and inventory-slot
                (get-in (:entity/inventory @player-eid) inventory-slot)) :cursors/hand-before-grab
           (actor/window-title-bar? actor) :cursors/move-window
           (actor/button?           actor) :cursors/over-button
           :else :cursors/default)))

      :interaction-state/clickable-mouseover-eid
      (let [{:keys [clicked-eid
                    in-click-range?]} params]
        (case (:type (:entity/clickable @clicked-eid))
          :clickable/item (if in-click-range?
                            :cursors/hand-before-grab
                            :cursors/hand-before-grab-gray)
          :clickable/player :cursors/bag))

      :interaction-state.skill/usable
      :cursors/use-skill

      :interaction-state.skill/not-usable
      :cursors/skill-not-usable

      :interaction-state/no-skill-selected
      :cursors/no-skill-selected)))

(defn ->txs [ctx player-eid]
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
