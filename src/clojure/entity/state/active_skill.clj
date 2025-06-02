(ns clojure.entity.state.active-skill
  (:require [clojure.ctx :as ctx]
            [clojure.effect :as effect]
            [clojure.entity :as entity]
            [clojure.state :as state]
            [clojure.timer :as timer]
            [clojure.utils :refer [defcomponent]]))

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
           (ctx/line-of-sight? ctx @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defn- render-active-effect [ctx effect-ctx effect]
  (mapcat #(effect/render % effect-ctx ctx) effect))

(defcomponent :active-skill
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

  (state/cursor [_] :cursors/sandclock)

  (state/pause-game? [_] false)

  (state/enter! [[_ {:keys [skill]}] eid]
    [[:tx/sound (:skill/start-action-sound skill)]
     (when (:skill/cooldown skill)
       [:tx/set-cooldown eid skill])
     (when (and (:skill/cost skill)
                (not (zero? (:skill/cost skill))))
       [:tx/pay-mana-cost eid (:skill/cost skill)])])

  (entity/render-info! [[_ {:keys [skill effect-ctx counter]}]
                        entity
                        {:keys [ctx/elapsed-time] :as ctx}]
    (let [{:keys [entity/image skill/effects]} skill]
      (concat (draw-skill-image image
                                entity
                                (entity/position entity)
                                (timer/ratio elapsed-time counter))
              (render-active-effect ctx
                                    effect-ctx ; TODO !!!
                                    ; !! FIXME !!
                                    ; (update-effect-ctx effect-ctx)
                                    ; - render does not need to update .. update inside active-skill
                                    effects)))))
