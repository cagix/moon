(ns cdq.entity.state.active-skill
  (:require [cdq.effect :as effect]
            [cdq.entity.stats :as stats]
            [cdq.timer :as timer]))

(defn- apply-action-speed-modifier [{:keys [entity/stats]} skill action-time]
  (/ action-time
     (or (stats/get-stat-value stats (:skill/action-time-modifier-key skill))
         1)))

(defn create [eid [skill effect-ctx] {:keys [world/elapsed-time]}]
  {:skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer/create elapsed-time))})

(defn enter [{:keys [skill]} eid]
  [[:tx/sound (:skill/start-action-sound skill)]
   [:tx/set-cooldown eid skill]
   [:tx/update eid :entity/stats stats/pay-mana-cost (:skill/cost skill)]])
