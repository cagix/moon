(ns forge.entity.states
  (:require [forge.controls :as controls]
            [forge.entity :refer [->v tick render-info]]
            [forge.entity.components :as entity]
            [forge.entity.state :as state]
            [forge.follow-ai :as follow-ai]
            [forge.graphics :refer [draw-filled-circle draw-sector draw-image]]
            [forge.ui.modal :as modal]
            [forge.world :as world :refer [timer stopped? line-of-sight? finished-ratio]]))

(defmethod ->v :npc-dead [[_ eid]]
  {:eid eid})

(defmethod state/enter :npc-dead [[_ {:keys [eid]}]]
  (swap! eid assoc :entity/destroyed? true))

(defn- nearest-enemy [entity]
  (world/nearest-entity @(world/cell (entity/tile entity))
                        (entity/enemy entity)))

(defn- npc-effect-ctx [eid]
  (let [entity @eid
        target (nearest-enemy entity)
        target (when (and target (line-of-sight? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target (entity/direction entity @target))}))

(comment
 (let [eid (world/ids->eids 76)
       effect-ctx (npc-effect-ctx eid)]
   (npc-choose-skill effect-ctx @eid))
 )

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (entity/skill-usable-state entity % ctx))
                     (effects-useful? ctx (:skill/effects %))))
       first))

(defmethods :npc-idle
  (->v [[_ eid]]
    {:eid eid})

  (tick [_ eid]
    (let [effect-ctx (npc-effect-ctx eid)]
      (if-let [skill (npc-choose-skill @eid effect-ctx)]
        (entity/event eid :start-action [skill effect-ctx])
        (entity/event eid :movement-direction (or (follow-ai/direction-vector eid) [0 0]))))))

; npc moving is basically a performance optimization so npcs do not have to check
; usable skills every frame
; also prevents fast twitching around changing directions every frame
(defmethods :npc-moving
  (->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector
     :counter (timer (* (entity/stat @eid :entity/reaction-time) 0.016))})

  (state/enter [{:keys [eid movement-vector]}]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))

  (state/exit [{:keys [eid]}]
    (swap! eid dissoc :entity/movement))

  (tick [{:keys [counter]} eid]
    (when (stopped? counter)
      (entity/event eid :timer-finished))))

(defmethods :npc-sleeping
  (->v [[_ eid]]
    {:eid eid})

  (state/exit [[_ {:keys [eid]}]]
    (world/shout (:position @eid) (:entity/faction @eid) 0.2)
    (swap! eid entity/add-text-effect "[WHITE]!"))

  (tick [_ eid]
    (let [entity @eid
          cell (world/cell (entity/tile entity))] ; pattern!
      (when-let [distance (world/nearest-entity-distance @cell (entity/enemy entity))]
        (when (<= distance (entity/stat entity :entity/aggro-range))
          (entity/event eid :alert))))))

(defmethods :player-dead
  (state/cursor [_]
    :cursors/black-x)

  (state/pause-game? [_]
    true)

  (state/enter [_]
    (play-sound "bfxr_playerdeath")
    (modal/show {:title "YOU DIED"
                 :text "\nGood luck next time"
                 :button-text ":("
                 :on-click #(change-screen :screens/main-menu)})))

(defmethods :stunned
  (->v [[_ eid duration]]
    {:eid eid
     :counter (timer duration)})

  (state/cursor [_]
    :cursors/denied)

  (state/pause-game? [_]
    false)

  (tick [[_ {:keys [counter]}] eid]
    (when (stopped? counter)
      (entity/event eid :effect-wears-off))))

(defmethods :player-moving
  (->v [[_ eid movement-vector]]
    {:eid eid
     :movement-vector movement-vector})

  (state/cursor [_]
    :cursors/walking)

  (state/pause-game? [_]
    false)

  (state/enter [[_ {:keys [eid movement-vector]}]]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)}))

  (state/exit [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement))

  (tick [[_ {:keys [movement-vector]}] eid]
    (if-let [movement-vector (controls/movement-vector)]
      (swap! eid assoc :entity/movement {:direction movement-vector
                                         :speed (entity/stat @eid :entity/movement-speed)})
      (entity/event eid :no-movement-input))))

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

(defmethods :active-skill
  (->v [[_ eid [skill effect-ctx]]]
    {:eid eid
     :skill skill
     :effect-ctx effect-ctx
     :counter (->> skill
                   :skill/action-time
                   (apply-action-speed-modifier @eid skill)
                   timer)})

  (state/cursor [_]
    :cursors/sandclock)

  (state/pause-game? [_]
    false)

  (state/enter [[_ {:keys [eid skill]}]]
    (play-sound (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill))))

  (tick [[_ {:keys [skill effect-ctx counter]}] eid]
    (cond
     (not (effects-applicable? (check-update-ctx effect-ctx)
                               (:skill/effects skill)))
     (do
      (entity/event eid :action-done)
      ; TODO some sound ?
      )

     (stopped? counter)
     (do
      (effects-do! effect-ctx (:skill/effects skill))
      (entity/event eid :action-done))))

  (render-info [[_ {:keys [skill effect-ctx counter]}] entity]
    (let [{:keys [entity/image skill/effects]} skill]
      (draw-skill-image image entity (:position entity) (finished-ratio counter))
      (effects-render (check-update-ctx effect-ctx) effects))))
