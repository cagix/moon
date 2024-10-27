(ns ^:no-doc moon.entity.active
  (:require [moon.component :refer [defc] :as component]
            [moon.effect :as effect]
            [moon.entity :as entity]
            [moon.graphics :as g]
            [moon.world :as world :refer [timer stopped? finished-ratio]]))

(defn- draw-skill-icon [icon entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions icon)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (g/draw-filled-circle center radius [1 1 1 0.125])
    (g/draw-sector center radius
                   90 ; start-angle
                   (* (float action-counter-ratio) 360) ; degree
                   [1 1 1 0.5])
    (g/draw-image icon [(- (float x) radius) y])))

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
           (world/line-of-sight? @source @target))
    ctx
    (dissoc ctx :effect/target)))

(defc :active-skill
  {:let {:keys [eid skill effect-ctx counter]}}
  (entity/->v [[_ eid [skill effect-ctx]]]
    {:eid eid
     :skill skill
     :effect-ctx effect-ctx
     :counter (->> skill
                   :skill/action-time
                   (apply-action-speed-modifier @eid skill)
                   timer)})

  (entity/player-enter [_]
    [[:tx/cursor :cursors/sandclock]])

  (entity/pause-game? [_]
    false)

  (entity/enter [_]
    [[:tx/sound (:skill/start-action-sound skill)]
     (when (:skill/cooldown skill)
       [:e/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] (timer (:skill/cooldown skill))])
     (when (and (:skill/cost skill)
                (zero? (:skill/cost skill)))
       [:tx.entity.stats/pay-mana-cost eid (:skill/cost skill)])])

  (entity/tick [_ eid]
    (cond
     (effect/with-ctx (check-update-ctx effect-ctx)
       (not (effect/applicable? (:skill/effects skill))))
     [[:tx/event eid :action-done]
      ; TODO some sound ?
      ]

     (stopped? counter)
     [[:tx/event eid :action-done]
      [:tx/effect effect-ctx (:skill/effects skill)]]))

  (entity/render-info [_ entity]
    (let [{:keys [entity/image skill/effects]} skill]
      (draw-skill-icon image entity (:position entity) (finished-ratio counter))
      (effect/with-ctx (check-update-ctx effect-ctx)
        (run! component/render effects)))))
