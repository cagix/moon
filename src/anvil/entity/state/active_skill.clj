(ns ^:no-doc anvil.entity.state.active-skill
  (:require [anvil.component :as component]
            [anvil.effect :as effect]
            [anvil.entity :as entity]
            [anvil.world :refer [timer finished-ratio stopped?]]
            [clojure.gdx.audio.sound :as sound]
            [gdl.graphics :as g]))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (g/filled-circle center radius [1 1 1 0.125])
    (g/sector center radius
              90 ; start-angle
              (* (float action-counter-ratio) 360) ; degree
              [1 1 1 0.5])
    (g/draw-image image [(- (float x) radius) y])))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defmethods :active-skill
  (component/->v [[_ eid [skill effect-ctx]]]
    {:eid eid
     :skill skill
     :effect-ctx effect-ctx
     :counter (->> skill
                   :skill/action-time
                   (apply-action-speed-modifier @eid skill)
                   timer)})

  (component/enter [[_ {:keys [eid skill]}]]
    (sound/play (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill))))

  (component/tick [[_ {:keys [skill effect-ctx counter]}] eid]
    (cond
     (not (effect/some-applicable? (effect/check-update-ctx effect-ctx)
                                   (:skill/effects skill)))
     (do
      (entity/event eid :action-done)
      ; TODO some sound ?
      )

     (stopped? counter)
     (do
      (effect/do-all! effect-ctx (:skill/effects skill))
      (entity/event eid :action-done))))

  (component/render-info [[_ {:keys [skill effect-ctx counter]}] entity]
    (let [{:keys [entity/image skill/effects]} skill]
      (draw-skill-image image
                        entity
                        (:position entity)
                        (finished-ratio counter))
      (effect/render-info (effect/check-update-ctx effect-ctx)
                          effects))))
