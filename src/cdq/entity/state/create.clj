(ns cdq.entity.state.create
  (:require [cdq.stats :as stats]
            [cdq.timer :as timer]))

(def reaction-time-multiplier 0.016)

(defn- apply-action-speed-modifier [{:keys [creature/stats]} skill action-time]
  (/ action-time
     (or (stats/get-stat-value stats (:skill/action-time-modifier-key skill))
         1)))

(def function-map
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
