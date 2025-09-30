(ns cdq.entity.state.active-skill
  (:require [cdq.effect :as effect]
            [cdq.entity.stats :as stats]
            [cdq.timer :as timer]
            [cdq.world :as world]))

(defn- update-effect-ctx
  [world {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (world/line-of-sight? world @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

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

(defn tick
  [{:keys [skill effect-ctx counter]}
   eid
   {:keys [world/elapsed-time]
    :as world}]
  (let [effect-ctx (update-effect-ctx world effect-ctx)]
    (cond
     (not (seq (filter #(effect/applicable? % effect-ctx)
                       (:skill/effects skill))))
     [[:tx/event eid :action-done]]

     (timer/stopped? elapsed-time counter)
     [[:tx/effect effect-ctx (:skill/effects skill)]
      [:tx/event eid :action-done]])))

(defn enter [{:keys [skill]} eid]
  [[:tx/sound (:skill/start-action-sound skill)]
   (when (:skill/cooldown skill)
     [:tx/set-cooldown eid skill])
   (when (and (:skill/cost skill)
              (not (zero? (:skill/cost skill))))
     [:tx/pay-mana-cost eid (:skill/cost skill)])])
