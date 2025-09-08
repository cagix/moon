(ns cdq.entity.state
  (:require [cdq.entity :as entity]
            [cdq.entity.state.player-item-on-cursor]
            [cdq.stats :as stats]
            [cdq.timer :as timer]))

(def reaction-time-multiplier 0.016)

(defn- apply-action-speed-modifier [{:keys [creature/stats]} skill action-time]
  (/ action-time
     (or (stats/get-stat-value stats (:skill/action-time-modifier-key skill))
         1)))

(def ->create
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

(defn create [ctx state-k eid params]
  {:pre [(keyword? state-k)]}
  (let [result (if-let [f (state-k ->create)]
                 (f eid params ctx)
                 (if params
                   params
                   :something ; nil components are not tick'ed1
                   ))]
    #_(binding [*print-level* 2]
        (println "result of create-state-v " state-k)
        (clojure.pprint/pprint result))
    result))

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
