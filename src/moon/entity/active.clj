(ns ^:no-doc moon.entity.active
  (:require [forge.assets :refer [play-sound]]
            [forge.app :refer [draw-image draw-filled-circle draw-sector]]
            [moon.systems.effect :as effect]
            [moon.effects :as effects]
            [moon.entity :as entity]
            [moon.world :refer [timer stopped? finished-ratio line-of-sight?]]))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (draw-filled-circle center radius [1 1 1 0.125])
    (draw-sector center radius
                 90 ; start-angle
                 (* (float action-counter-ratio) 360) ; degree
                 [1 1 1 0.5])
    (draw-image image [(- (float x) radius) y])))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
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

(defn ->v [eid [skill effect-ctx]]
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

(defn enter [{:keys [eid skill]}]
  (play-sound (:skill/start-action-sound skill))
  (when (:skill/cooldown skill)
    (swap! eid assoc-in
           [:entity/skills (:property/id skill) :skill/cooling-down?]
           (timer (:skill/cooldown skill))))
  (when (and (:skill/cost skill)
             (not (zero? (:skill/cost skill))))
    (swap! eid entity/pay-mana-cost (:skill/cost skill))))

(defn tick [{:keys [skill effect-ctx counter]} eid]
  (cond
   (not (effects/applicable? (check-update-ctx effect-ctx)
                             (:skill/effects skill)))
   (do
    (entity/event eid :action-done)
    ; TODO some sound ?
    )

   (stopped? counter)
   (do
    (effects/do! effect-ctx (:skill/effects skill))
    (entity/event eid :action-done))))

(defn render-info [{:keys [skill effect-ctx counter]} entity]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image image entity (:position entity) (finished-ratio counter))
    (run! #(effect/render % (check-update-ctx effect-ctx)) effects)))
