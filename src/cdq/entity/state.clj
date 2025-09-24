(ns cdq.entity.state
  (:require [cdq.entity :as entity]
            cdq.entity.state.player-idle
            cdq.entity.state.player-item-on-cursor
            cdq.entity.state.player-moving
            [cdq.stats :as stats]
            [cdq.timer :as timer])
  (:import (clojure.lang APersistentVector)))

(defprotocol State
  (create       [_ eid ctx])
  (handle-input [_ eid ctx])
  (cursor       [_ eid ctx])
  (enter        [_ eid])
  (exit         [_ eid ctx])
  (clicked-inventory-cell [_ eid cell])
  (draw-gui-view [_ eid ctx]))

(defn- apply-action-speed-modifier [{:keys [creature/stats]} skill action-time]
  (/ action-time
     (or (stats/get-stat-value stats (:skill/action-time-modifier-key skill))
         1)))

(def ^:private reaction-time-multiplier 0.016)

(def ^:private fn->k->var
  {
   :create {:active-skill (fn [eid [skill effect-ctx] {:keys [ctx/world]}]
                            {:skill skill
                             :effect-ctx effect-ctx
                             :counter (->> skill
                                           :skill/action-time
                                           (apply-action-speed-modifier @eid skill)
                                           (timer/create (:world/elapsed-time world)))})
            :npc-moving (fn [eid movement-vector {:keys [ctx/world]}]
                          {:movement-vector movement-vector
                           :timer (timer/create (:world/elapsed-time world)
                                                (* (stats/get-stat-value (:creature/stats @eid) :entity/reaction-time)
                                                   reaction-time-multiplier))})
            :player-item-on-cursor (fn [_eid item _ctx]
                                     {:item item})
            :player-moving (fn [eid movement-vector _ctx]
                             {:movement-vector movement-vector})
            :stunned (fn [_eid duration {:keys [ctx/world]}]
                       {:counter (timer/create (:world/elapsed-time world) duration)})}

   :enter {:npc-dead (fn [_ eid]
                       [[:tx/mark-destroyed eid]])
           :npc-moving (fn [{:keys [movement-vector]} eid]
                         [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                           :speed (or (stats/get-stat-value (:creature/stats @eid) :entity/movement-speed)
                                                                      0)}]])
           :player-dead (fn [_ _eid]
                          [[:tx/sound "bfxr_playerdeath"]
                           [:tx/show-modal {:title "YOU DIED - again!"
                                            :text "Good luck next time!"
                                            :button-text "OK"
                                            :on-click (fn [])}]])
           :player-item-on-cursor (fn [{:keys [item]} eid]
                                    [[:tx/assoc eid :entity/item-on-cursor item]])
           :player-moving (fn [{:keys [movement-vector]} eid]
                            [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                              :speed (or (stats/get-stat-value (:creature/stats @eid) :entity/movement-speed)
                                                                         0)}]])
           :active-skill (fn [{:keys [skill]} eid]
                           [[:tx/sound (:skill/start-action-sound skill)]
                            (when (:skill/cooldown skill)
                              [:tx/set-cooldown eid skill])
                            (when (and (:skill/cost skill)
                                       (not (zero? (:skill/cost skill))))
                              [:tx/pay-mana-cost eid (:skill/cost skill)])])}

   :exit {:npc-moving            (fn [_ eid _ctx]
                                   [[:tx/dissoc eid :entity/movement]])
          :npc-sleeping          (fn [_ eid _ctx]
                                   [[:tx/spawn-alert (entity/position @eid) (:entity/faction @eid) 0.2]
                                    [:tx/add-text-effect eid "[WHITE]!" 1]])
          :player-item-on-cursor (fn [_ eid {:keys [ctx/graphics]}]
                                   ; at clicked-cell when we put it into a inventory-cell
                                   ; we do not want to drop it on the ground too additonally,
                                   ; so we dissoc it there manually. Otherwise it creates another item
                                   ; on the ground
                                   (let [entity @eid]
                                     (when (:entity/item-on-cursor entity)
                                       [[:tx/sound "bfxr_itemputground"]
                                        [:tx/dissoc eid :entity/item-on-cursor]
                                        [:tx/spawn-item
                                         (cdq.entity.state.player-item-on-cursor/item-place-position
                                          (:graphics/world-mouse-position graphics)
                                          entity)
                                         (:entity/item-on-cursor entity)]])))
          :player-moving         (fn [_ eid _ctx]
                                   [[:tx/dissoc eid :entity/movement]])}

   :cursor {:active-skill :cursors/sandclock
            :player-dead :cursors/black-x
            :player-idle cdq.entity.state.player-idle/cursor
            :player-item-on-cursor :cursors/hand-grab
            :player-moving :cursors/walking
            :stunned :cursors/denied}

   :handle-input {:player-idle           cdq.entity.state.player-idle/handle-input
                  :player-item-on-cursor cdq.entity.state.player-item-on-cursor/handle-input
                  :player-moving         cdq.entity.state.player-moving/handle-input}

   :clicked-inventory-cell {:player-idle           cdq.entity.state.player-idle/clicked-inventory-cell
                            :player-item-on-cursor cdq.entity.state.player-item-on-cursor/clicked-inventory-cell}

   :draw-gui-view {:player-item-on-cursor cdq.entity.state.player-item-on-cursor/draw-gui-view}
   })

(extend APersistentVector
  State
  {:create (fn [[k v] eid ctx]
             (if-let [f (k (:create fn->k->var))]
               (f eid v ctx)
               (or v :something))) ; nil components are not tick'ed

   :handle-input (fn [[k v] eid ctx]
                   (if-let [f (k (:handle-input fn->k->var))]
                     (f eid ctx)
                     nil))

   :cursor (fn [[k v] eid ctx]
             (let [->cursor (k (:cursor fn->k->var))]
               (if (keyword? ->cursor)
                 ->cursor
                 (->cursor eid ctx))))

   :enter (fn [[k v] eid]
            (when-let [f (k (:enter fn->k->var))]
              (f v eid)))

   :exit (fn [[k v] eid ctx]
           (when-let [f (k (:exit fn->k->var))]
             (f v eid ctx)))

   :clicked-inventory-cell (fn [[k v] eid cell]
                             (when-let [f (k (:clicked-inventory-cell fn->k->var))]
                               (f eid cell)))

   :draw-gui-view (fn [[k] eid ctx]
                    (when-let [f (k (:draw-gui-view fn->k->var))]
                      (f eid ctx)))})
