(ns cdq.entity.state.active-skill
  (:require [cdq.entity :as entity]
            [cdq.context :as world]
            [gdl.context.timer :as timer]
            [cdq.effect-context :as effect-ctx]
            [clojure.gdx :refer [play]]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [context {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (world/line-of-sight? context @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn cursor [_]
  :cursors/sandclock)

(defn pause-game? [_]
  false)

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defn create [[_ eid [skill effect-ctx]] c]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer/create c))})

(defn enter [[_ {:keys [eid skill]}] c]
  (play (:skill/start-action-sound skill))
  (when (:skill/cooldown skill)
    (swap! eid assoc-in
           [:entity/skills (:property/id skill) :skill/cooling-down?]
           (timer/create c (:skill/cooldown skill))))
  (when (and (:skill/cost skill)
             (not (zero? (:skill/cost skill))))
    (swap! eid entity/pay-mana-cost (:skill/cost skill))))

(defn tick [[_ {:keys [skill effect-ctx counter]}] eid c]
  (cond
   (not (effect-ctx/some-applicable? (update-effect-ctx c effect-ctx)
                                     (:skill/effects skill)))
   (do
    (world/send-event! c eid :action-done)
    ; TODO some sound ?
    )

   (timer/stopped? c counter)
   (do
    (effect-ctx/do-all! c effect-ctx (:skill/effects skill))
    (world/send-event! c eid :action-done))))
