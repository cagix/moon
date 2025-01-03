(ns cdq.entity.state.active-skill
  (:require [cdq.entity :as entity]
            [cdq.context :refer [line-of-sight? timer finished-ratio stopped?]]
            [cdq.effect-context :as effect-ctx]
            [clojure.gdx :refer [play]]
            [gdl.context :as c]))

(defn cursor [_]
  :cursors/sandclock)

(defn pause-game? [_]
  false)

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defn- draw-skill-image [c image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (c/filled-circle c center radius [1 1 1 0.125])
    (c/sector c
              center
              radius
              90 ; start-angle
              (* (float action-counter-ratio) 360) ; degree
              [1 1 1 0.5])
    (c/draw-image c image [(- (float x) radius) y])))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- check-update-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [context {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (line-of-sight? context @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn create [[_ eid [skill effect-ctx]] c]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer c))})

(defn enter [[_ {:keys [eid skill]}] c]
  (play (:skill/start-action-sound skill))
  (when (:skill/cooldown skill)
    (swap! eid assoc-in
           [:entity/skills (:property/id skill) :skill/cooling-down?]
           (timer c (:skill/cooldown skill))))
  (when (and (:skill/cost skill)
             (not (zero? (:skill/cost skill))))
    (swap! eid entity/pay-mana-cost (:skill/cost skill))))

(defn tick [[_ {:keys [skill effect-ctx counter]}] eid c]
  (cond
   (not (effect-ctx/some-applicable? (check-update-ctx c effect-ctx)
                                     (:skill/effects skill)))
   (do
    (entity/event c eid :action-done)
    ; TODO some sound ?
    )

   (stopped? c counter)
   (do
    (effect-ctx/do-all! c effect-ctx (:skill/effects skill))
    (entity/event c eid :action-done))))

(defn render-info [[_ {:keys [skill effect-ctx counter]}] entity c]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image c
                      image
                      entity
                      (:position entity)
                      (finished-ratio c counter))
    (effect-ctx/render-info c
                            (check-update-ctx c effect-ctx)
                            effects)))
