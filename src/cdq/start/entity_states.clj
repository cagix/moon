(ns cdq.start.entity-states
  (:require [cdq.controls :as controls]
            [cdq.entity :as entity]
            [cdq.entity.state]
            [cdq.entity.state.player-idle]
            [cdq.entity.state.player-item-on-cursor]
            [cdq.inventory :as inventory]
            [cdq.stats :as stats]
            [cdq.timer :as timer]
            [cdq.ui.windows.inventory :as inventory-window]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.ui.button :as button]
            [clojure.vis-ui.window :as window]))

(defn- speed [{:keys [creature/stats]}]
  (or (stats/get-stat-value stats :entity/movement-speed)
      0))

(defn inventory-window-visible? [stage]
  (-> stage :windows :inventory-window actor/visible?))

(defn can-pickup-item? [entity item]
  (inventory/can-pickup-item? (:entity/inventory entity) item))

(defn interaction-state->txs [ctx player-eid]
  (let [[k params] (cdq.entity.state.player-idle/interaction-state ctx player-eid)]
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

(def reaction-time-multiplier 0.016)

(defn- apply-action-speed-modifier [{:keys [creature/stats]} skill action-time]
  (/ action-time
     (or (stats/get-stat-value stats (:skill/action-time-modifier-key skill))
         1)))

(def state->create
  {:active-skill          (fn [eid [skill effect-ctx] {:keys [ctx/elapsed-time]}]
                            {:skill skill
                             :effect-ctx effect-ctx
                             :counter (->> skill
                                           :skill/action-time
                                           (apply-action-speed-modifier @eid skill)
                                           (timer/create elapsed-time))})
   :npc-moving            (fn [eid movement-vector {:keys [ctx/elapsed-time]}]
                            {:movement-vector movement-vector
                             :timer (timer/create elapsed-time
                                                  (* (stats/get-stat-value (:creature/stats @eid) :entity/reaction-time)
                                                     reaction-time-multiplier))})
   :player-item-on-cursor (fn [_eid item _ctx]
                            {:item item})
   :player-moving         (fn [eid movement-vector _ctx]
                            {:movement-vector movement-vector})
   :stunned               (fn [_eid duration {:keys [ctx/elapsed-time]}]
                            {:counter (timer/create elapsed-time duration)})})

(def state->enter {:npc-dead              (fn [_ eid]
                                     [[:tx/mark-destroyed eid]])
            :npc-moving            (fn [{:keys [movement-vector]} eid]
                                     [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                                       :speed (or (stats/get-stat-value (:creature/stats @eid) :entity/movement-speed)
                                                                                  0)}]])
            :player-dead           (fn [_ _eid]
                                     [[:tx/sound "bfxr_playerdeath"]
                                      [:tx/show-modal {:title "YOU DIED - again!"
                                                       :text "Good luck next time!"
                                                       :button-text "OK"
                                                       :on-click (fn [])}]])
            :player-item-on-cursor (fn [{:keys [item]} eid]
                                     [[:tx/assoc eid :entity/item-on-cursor item]])
            :player-moving         (fn [{:keys [movement-vector]} eid]
                                     [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                                       :speed (or (stats/get-stat-value (:creature/stats @eid) :entity/movement-speed)
                                                                                  0)}]])
            :active-skill          (fn [{:keys [skill]} eid]
                                     [[:tx/sound (:skill/start-action-sound skill)]
                                      (when (:skill/cooldown skill)
                                        [:tx/set-cooldown eid skill])
                                      (when (and (:skill/cost skill)
                                                 (not (zero? (:skill/cost skill))))
                                        [:tx/pay-mana-cost eid (:skill/cost skill)])])})

(def state->cursor
  {:active-skill :cursors/sandclock
   :player-dead :cursors/black-x
   :player-idle (fn [player-eid ctx]
                  (let [[k params] (cdq.entity.state.player-idle/interaction-state ctx player-eid)]
                    (case k
                      :interaction-state/mouseover-actor
                      (let [actor params]
                        (let [inventory-slot (inventory-window/cell-with-item? actor)]
                          (cond
                           (and inventory-slot
                                (get-in (:entity/inventory @player-eid) inventory-slot)) :cursors/hand-before-grab
                           (window/title-bar? actor) :cursors/move-window
                           (button/is?        actor) :cursors/over-button
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
   :player-item-on-cursor :cursors/hand-grab
   :player-moving :cursors/walking
   :stunned :cursors/denied})

(def state->exit
  {:npc-moving            (fn [_ eid _ctx]
                            [[:tx/dissoc eid :entity/movement]])
   :npc-sleeping          (fn [_ eid _ctx]
                            [[:tx/spawn-alert (entity/position @eid) (:entity/faction @eid) 0.2]
                             [:tx/add-text-effect eid "[WHITE]!" 1]])
   :player-item-on-cursor (fn [_ eid {:keys [ctx/world-mouse-position]}]
                            ; at clicked-cell when we put it into a inventory-cell
                            ; we do not want to drop it on the ground too additonally,
                            ; so we dissoc it there manually. Otherwise it creates another item
                            ; on the ground
                            (let [entity @eid]
                              (when (:entity/item-on-cursor entity)
                                [[:tx/sound "bfxr_itemputground"]
                                 [:tx/dissoc eid :entity/item-on-cursor]
                                 [:tx/spawn-item
                                  (cdq.entity.state.player-item-on-cursor/item-place-position world-mouse-position entity)
                                  (:entity/item-on-cursor entity)]])))
   :player-moving         (fn [_ eid _ctx]
                            [[:tx/dissoc eid :entity/movement]])})

(def state->handle-input
  {:player-idle           (fn [player-eid {:keys [ctx/input] :as ctx}]
                            (if-let [movement-vector (controls/player-movement-vector input)]
                              [[:tx/event player-eid :movement-input movement-vector]]
                              (when (input/button-just-pressed? input :left)
                                (interaction-state->txs ctx player-eid))))

   :player-item-on-cursor (fn [eid
                               {:keys [ctx/input
                                       ctx/mouseover-actor]}]
                            (when (and (input/button-just-pressed? input :left)
                                       (cdq.entity.state.player-item-on-cursor/world-item? mouseover-actor))
                              [[:tx/event eid :drop-item]]))
   :player-moving         (fn [eid {:keys [ctx/input]}]
                            (if-let [movement-vector (controls/player-movement-vector input)]
                              [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                                :speed (speed @eid)}]]
                              [[:tx/event eid :no-movement-input]]))})

(defn do! [ctx]
  (.bindRoot #'cdq.entity.state/->create state->create)
  (.bindRoot #'cdq.entity.state/state->enter state->enter)
  (.bindRoot #'cdq.entity.state/state->cursor state->cursor)
  (.bindRoot #'cdq.entity.state/state->exit state->exit)
  (.bindRoot #'cdq.entity.state/state->handle-input state->handle-input)
  ctx)
