(ns moon.entity.active
  (:require [gdl.graphics.image :as image]
            [gdl.graphics.shape-drawer :as sd]
            [moon.component :as component]
            [moon.effect :as effect]
            [moon.entity.modifiers :as mods]
            [moon.world.line-of-sight :refer [line-of-sight?]]
            [moon.world.time :refer [timer stopped? finished-ratio]]))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (sd/filled-circle center radius [1 1 1 0.125])
    (sd/sector center radius
                   90 ; start-angle
                   (* (float action-counter-ratio) 360) ; degree
                   [1 1 1 0.5])
    (image/draw image [(- (float x) radius) y])))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (mods/value entity (:skill/action-time-modifier-key skill))
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

(defn ->v [[_ eid [skill effect-ctx]]]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 timer)})

(defn player-enter [_]
  [[:tx/cursor :cursors/sandclock]])

(defn pause-game? [_]
  false)

(defn enter [[_ {:keys [eid skill]}]]
  [[:tx/sound (:skill/start-action-sound skill)]

   (when (:skill/cooldown skill)
     [:e/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] (timer (:skill/cooldown skill))])

   (when (and (:skill/cost skill)
              (not (zero? (:skill/cost skill))))
     [:tx.entity.stats/pay-mana-cost eid (:skill/cost skill)])])

(defn tick [[_ {:keys [skill effect-ctx counter]}] eid]
  (cond
   (effect/with-ctx (check-update-ctx effect-ctx)
     (not (effect/applicable? (:skill/effects skill))))
   [[:entity/fsm eid :action-done]
    ; TODO some sound ?
    ]

   (stopped? counter)
   [[:entity/fsm eid :action-done]
    [:tx/effect effect-ctx (:skill/effects skill)]]))

(defn render-info [[_ {:keys [skill effect-ctx counter]}] entity]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image image entity (:position entity) (finished-ratio counter))
    (effect/with-ctx (check-update-ctx effect-ctx)
      (run! component/render effects))))
