(ns ^:no-doc anvil.entity.state.active-skill
  (:require [anvil.effect :as effect]
            [anvil.entity :as entity]
            [cdq.context :refer [timer finished-ratio stopped?]]
            [clojure.gdx :refer [play]]
            [clojure.component :refer [defcomponent]]
            [gdl.context :as c]))

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

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defcomponent :active-skill
  (entity/->v [[_ eid [skill effect-ctx]] c]
    {:eid eid
     :skill skill
     :effect-ctx effect-ctx
     :counter (->> skill
                   :skill/action-time
                   (apply-action-speed-modifier @eid skill)
                   (timer c))})

  (entity/enter [[_ {:keys [eid skill]}] c]
    (play (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer c (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill))))

  (entity/tick [[_ {:keys [skill effect-ctx counter]}] eid c]
    (cond
     (not (effect/some-applicable? (effect/check-update-ctx c effect-ctx)
                                   (:skill/effects skill)))
     (do
      (entity/event c eid :action-done)
      ; TODO some sound ?
      )

     (stopped? c counter)
     (do
      (effect/do-all! c effect-ctx (:skill/effects skill))
      (entity/event c eid :action-done))))

  (entity/render-info [[_ {:keys [skill effect-ctx counter]}] entity c]
    (let [{:keys [entity/image skill/effects]} skill]
      (draw-skill-image c
                        image
                        entity
                        (:position entity)
                        (finished-ratio c counter))
      (effect/render-info c
                          (effect/check-update-ctx c effect-ctx)
                          effects))))
