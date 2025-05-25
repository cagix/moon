(ns cdq.entity.state.active-skill
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.state :as state]
            [gdl.utils :refer [defcomponent]]))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    [[:draw/filled-circle center radius [1 1 1 0.125]]
     [:draw/sector
      center
      radius
      90 ; start-angle
      (* (float action-counter-ratio) 360) ; degree
      [1 1 1 0.5]]
     [:draw/image image [(- (float x) radius) y]]]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [ctx {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (g/line-of-sight? ctx @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defn- render-active-effect [ctx effect-ctx effect]
  (mapcat #(effect/render % effect-ctx ctx) effect))

(defcomponent :active-skill
  (entity/create [[_ eid [skill effect-ctx]] ctx]
    {:skill skill
     :effect-ctx effect-ctx
     :counter (->> skill
                   :skill/action-time
                   (apply-action-speed-modifier @eid skill)
                   (g/create-timer ctx))})

  (entity/tick! [[_ {:keys [skill effect-ctx counter]}] eid ctx]
    (cond
     (not (effect/some-applicable? (update-effect-ctx ctx effect-ctx) ; TODO how 2 test
                                   (:skill/effects skill)))
     [[:tx/event eid :action-done]
      ; TODO some sound ?
      ]

     (g/timer-stopped? ctx counter)
     [[:tx/effect effect-ctx (:skill/effects skill)]
      [:tx/event eid :action-done]]))

  (state/cursor [_] :cursors/sandclock)

  (state/pause-game? [_] false)

  (state/enter! [[_ {:keys [skill]}] eid]
    [[:tx/sound (:skill/start-action-sound skill)]
     (when (:skill/cooldown skill)
       [:tx/set-cooldown eid skill])
     (when (and (:skill/cost skill)
                (not (zero? (:skill/cost skill))))
       [:tx/pay-mana-cost eid (:skill/cost skill)])])

  (entity/render-info! [[_ {:keys [skill effect-ctx counter]}] entity ctx]
    (let [{:keys [entity/image skill/effects]} skill]
      (concat (draw-skill-image image
                                entity
                                (entity/position entity)
                                (g/timer-ratio ctx counter))
              (render-active-effect ctx
                                    effect-ctx ; TODO !!!
                                    ; !! FIXME !!
                                    ; (update-effect-ctx effect-ctx)
                                    ; - render does not need to update .. update inside active-skill
                                    effects)))))
