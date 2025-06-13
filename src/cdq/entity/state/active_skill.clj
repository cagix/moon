(ns cdq.entity.state.active-skill
  (:require [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.timer :as timer]
            [cdq.utils :refer [defmethods]]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [ctx {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (ctx/line-of-sight? ctx @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defmethods :active-skill
  (entity/create [[_ eid [skill effect-ctx]]
                  {:keys [ctx/elapsed-time]}]
    {:skill skill
     :effect-ctx effect-ctx
     :counter (->> skill
                   :skill/action-time
                   (apply-action-speed-modifier @eid skill)
                   (timer/create elapsed-time))})

  (entity/tick! [[_ {:keys [skill effect-ctx counter]}]
                 eid
                 {:keys [ctx/elapsed-time] :as ctx}]
    (cond
     (not (effect/some-applicable? (update-effect-ctx ctx effect-ctx) ; TODO how 2 test
                                   (:skill/effects skill)))
     [[:tx/event eid :action-done]
      ; TODO some sound ?
      ]

     (timer/stopped? elapsed-time counter)
     [[:tx/effect effect-ctx (:skill/effects skill)]
      [:tx/event eid :action-done]]))

  (state/enter! [[_ {:keys [skill]}] eid]
    [[:tx/sound (:skill/start-action-sound skill)]
     (when (:skill/cooldown skill)
       [:tx/set-cooldown eid skill])
     (when (and (:skill/cost skill)
                (not (zero? (:skill/cost skill))))
       [:tx/pay-mana-cost eid (:skill/cost skill)])]))
