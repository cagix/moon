(ns cdq.ctx.interaction-state
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.windows.inventory :as inventory-window]
            [gdl.math.vector2 :as v]
            [gdl.ui :as ui]
            [gdl.c :as c]))

(defmulti on-clicked
  (fn [ctx eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [{:keys [ctx/player-eid
                                               ctx/stage]}
                                       eid]
  (let [item (:entity/item @eid)]
    (cond
     (-> stage :windows :inventory-window ui/visible?)
     [[:tx/sound "bfxr_takeit"]
      [:tx/mark-destroyed eid]
      [:tx/event player-eid :pickup-item item]]

     (inventory/can-pickup-item? (:entity/inventory @player-eid) item)
     [[:tx/sound "bfxr_pickup"]
      [:tx/mark-destroyed eid]
      [:tx/pickup-item player-eid item]]

     :else
     [[:tx/sound "bfxr_denied"]
      [:tx/show-message "Your Inventory is full"]])))

(defmethod on-clicked :clickable/player [_ctx _eid]
  [[:tx/toggle-inventory-visible]])

(defn- clickable->cursor [entity & {:keys [too-far-away?]}]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn distance [a b]
  (v/distance (entity/position a)
              (entity/position b)))

(defn in-click-range? [player-entity clicked-entity]
  (< (distance player-entity clicked-entity)
     (:entity/click-distance-tiles player-entity)))

(defn clickable-entity-interaction [ctx player-entity clicked-eid]
  (if (in-click-range? player-entity @clicked-eid)
    [(clickable->cursor @clicked-eid :too-far-away? false)
     (on-clicked ctx clicked-eid)]
    [(clickable->cursor @clicked-eid :too-far-away? true)
     [[:tx/sound "bfxr_denied"]
      [:tx/show-message "Too far away"]]]))

(defn interaction-state [{:keys [ctx/mouseover-eid] :as ctx}]
  (let [mouseover-actor (c/mouseover-actor ctx)]
    (cond
     mouseover-actor
     [:interaction-state/mouseover-actor mouseover-actor]

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     [:interaction-state/clickable-mouseover-eid mouseover-eid]

     :else
     [:interaction-state/try-use-skill])))

(defn- try-use-skill [{:keys [ctx/stage] :as ctx} eid]
  (if-let [skill-id (action-bar/selected-skill (:action-bar stage))]
    (let [entity @eid
          skill (skill-id (:entity/skills entity))
          effect-ctx (ctx/player-effect-ctx ctx eid)
          state (entity/skill-usable-state entity skill effect-ctx)]
      (if (= state :usable)
        [:try-use-skill/usable [skill effect-ctx]]
        [:try-use-skill/not-usable state]))
    [:try-use-skill/no-skill-selected]))

(defn- mouseover-actor->cursor [actor player-entity-inventory]
  (let [inventory-slot (inventory-window/cell-with-item? actor)]
    (cond
     (and inventory-slot
         (get-in player-entity-inventory inventory-slot)) :cursors/hand-before-grab
     (ui/window-title-bar? actor) :cursors/move-window
     (ui/button? actor) :cursors/over-button
     :else :cursors/default)))

(defn ->cursor [player-eid ctx]
  (let [[k params] (interaction-state ctx)]
    (case k
      :interaction-state/mouseover-actor (let [mouseover-actor params]
                                           (mouseover-actor->cursor mouseover-actor (:entity/inventory @player-eid)))
      :interaction-state/clickable-mouseover-eid (let [mouseover-eid params
                                                       [cursor _txs] (clickable-entity-interaction ctx @player-eid mouseover-eid)]
                                                   cursor)
      :interaction-state/try-use-skill (let [[k params] (try-use-skill ctx player-eid)]
                                         (case k
                                           :try-use-skill/no-skill-selected :cursors/no-skill-selected
                                           :try-use-skill/usable            :cursors/use-skill
                                           :try-use-skill/not-usable        :cursors/skill-not-usable)))))

(defn ->txs [ctx player-eid]
  (let [[k params] (interaction-state ctx)]
    (case k
      :interaction-state/mouseover-actor nil ; handled by ui actors themself.
      :interaction-state/clickable-mouseover-eid (let [mouseover-eid params
                                                       [_cursor txs] (clickable-entity-interaction ctx @player-eid mouseover-eid)]
                                                   txs)
      :interaction-state/try-use-skill (let [[k params] (try-use-skill ctx player-eid)]
                                         (case k
                                           :try-use-skill/no-skill-selected [[:tx/sound "bfxr_denied"]
                                                                             [:tx/show-message "No selected skill"]]
                                           :try-use-skill/usable (let [[skill effect-ctx] params]
                                                                   [[:tx/event player-eid :start-action [skill effect-ctx]]])
                                           :try-use-skill/not-usable (let [state params]
                                                                       [[:tx/sound "bfxr_denied"]
                                                                        [:tx/show-message (case state
                                                                                            :cooldown "Skill is still on cooldown"
                                                                                            :not-enough-mana "Not enough mana"
                                                                                            :invalid-params "Cannot use this here")]]))))))
