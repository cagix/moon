(ns forge.entity.state.active-skill
  (:require [anvil.app :refer [play-sound]]
            [anvil.effect :refer [effects-applicable? effects-do! effects-render]]
            [anvil.entity :refer [stat-value send-event pay-mana-cost]]
            [anvil.graphics :as g :refer [draw-image]]
            [anvil.world :refer [stopped? timer finished-ratio]]
            [forge.world :refer [line-of-sight?]]))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (stat-value entity (:skill/action-time-modifier-key skill))
         1)))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- check-update-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [{:keys [effect/source effect/target] :as ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (line-of-sight? @source @target))
    ctx
    (dissoc ctx :effect/target)))

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
    (draw-image image [(- (float x) radius) y])))

(defn ->v [[_ eid [skill effect-ctx]]]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 timer)})

(defn cursor [_]
  :cursors/sandclock)

(defn pause-game? [_]
  false)

(defn enter [[_ {:keys [eid skill]}]]
  (play-sound (:skill/start-action-sound skill))
  (when (:skill/cooldown skill)
    (swap! eid assoc-in
           [:entity/skills (:property/id skill) :skill/cooling-down?]
           (timer (:skill/cooldown skill))))
  (when (and (:skill/cost skill)
             (not (zero? (:skill/cost skill))))
    (swap! eid pay-mana-cost (:skill/cost skill))))

(defn tick [[_ {:keys [skill effect-ctx counter]}] eid]
  (cond
   (not (effects-applicable? (check-update-ctx effect-ctx)
                             (:skill/effects skill)))
   (do
    (send-event eid :action-done)
    ; TODO some sound ?
    )

   (stopped? counter)
   (do
    (effects-do! effect-ctx (:skill/effects skill))
    (send-event eid :action-done))))

(defn render-info [[_ {:keys [skill effect-ctx counter]}] entity]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image image entity (:position entity) (finished-ratio counter))
    (effects-render (check-update-ctx effect-ctx) effects)))
