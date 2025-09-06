(ns cdq.tx.state-enter
  (:require [cdq.stats :as stats]))

(def enter {:npc-dead              (fn [_ eid]
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

(defn do! [[_ eid [state-k state-v]] _ctx]
  (when-let [f (state-k enter)]
    (f state-v eid)))
