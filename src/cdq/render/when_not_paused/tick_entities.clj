(ns cdq.render.when-not-paused.tick-entities
  (:require [cdq.context :as context]
            [cdq.effect-context :as effect-ctx]
            [cdq.entity :as entity]
            [cdq.entity.fsm :as fsm]
            cdq.error
            [gdl.graphics.animation :as animation]
            [cdq.grid :as grid]
            [cdq.schema :as s]
            [cdq.skill :as skill]
            [cdq.line-of-sight :as los]
            [cdq.timer :as timer]
            [gdl.math.vector2 :as v]
            [cdq.world.potential-field :as potential-field]
            [gdl.utils :refer [find-first]]
            [cdq.world :refer [nearest-enemy
                               player-movement-vector
                               friendlies-in-radius
                               minimum-size]]))

(defmulti tick! (fn [[k] eid c]
                  k))
(defmethod tick! :default [_ eid c])

(defn render [{:keys [cdq.game/active-entities] :as context}]
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (tick! [k v] eid context))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (cdq.error/error-window context t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  context)

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [context {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (los/exists? context @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defmethod tick! :active-skill [[_ {:keys [skill effect-ctx counter]}]
                                eid
                                {:keys [cdq.context/elapsed-time] :as c}]
  (cond
   (not (effect-ctx/some-applicable? (update-effect-ctx c effect-ctx)
                                     (:skill/effects skill)))
   (do
    (fsm/event c eid :action-done)
    ; TODO some sound ?
    )

   (timer/stopped? counter elapsed-time)
   (do
    (effect-ctx/do-all! c effect-ctx (:skill/effects skill))
    (fsm/event c eid :action-done))))

(defn- npc-choose-skill [c entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect-ctx/applicable-and-useful? c ctx (:skill/effects %))))
       first))

(defn- npc-effect-context [c eid]
  (let [entity @eid
        target (nearest-enemy c entity)
        target (when (and target
                          (los/exists? c entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defmethod tick! :npc-idle [_ eid c]
  (let [effect-ctx (npc-effect-context c eid)]
    (if-let [skill (npc-choose-skill c @eid effect-ctx)]
      (fsm/event c eid :start-action [skill effect-ctx])
      (fsm/event c eid :movement-direction (or (potential-field/find-direction c eid) [0 0])))))

(defmethod tick! :npc-moving [[_ {:keys [counter]}]
                              eid
                              {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (fsm/event c eid :timer-finished)))

(defmethod tick! :npc-sleeping [_ eid {:keys [cdq.context/grid] :as c}]
  (let [entity @eid
        cell (grid (entity/tile entity))]
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (fsm/event c eid :alert)))))

(defmethod tick! :player-moving [[_ {:keys [movement-vector]}] eid c]
  (if-let [movement-vector (player-movement-vector)]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)})
    (fsm/event c eid :no-movement-input)))

(defmethod tick! :stunned [[_ {:keys [counter]}]
                           eid
                           {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (fsm/event c eid :effect-wears-off)))

(defmethod tick! :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}]
   eid
   {:keys [cdq.context/grid
           cdq.context/elapsed-time]
    :as c}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius grid (:position @eid) faction)]
      (fsm/event c friendly-eid :alert))))

(defmethod tick! :entity/animation
  [[k animation] eid {:keys [cdq.context/delta-time]}]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation delta-time)))))

(defmethod tick! :entity/delete-after-duration
  [[_ counter]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid assoc :entity/destroyed? true)))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [grid {:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (grid/rectangle->cells grid body))]
    (and (not-any? #(grid/blocked? % z-order) cells*)
         (->> cells*
              grid/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (entity/collides? other-entity body)))))))))

(defn- try-move [grid body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? grid new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [grid body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move grid body movement)
        (try-move grid body (assoc movement :direction [xdir 0]))
        (try-move grid body (assoc movement :direction [0 ydir])))))

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ minimum-size
                            cdq.time/max-delta)) ; need to make var because s/schema would fail later if divide / is inside the schema-form

(def speed-schema (s/m-schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod tick! :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
   eid
   {:keys [cdq.context/delta-time
           cdq.context/grid] :as context}]
  (assert (s/validate speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v/length direction))
              (v/normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v/length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time delta-time)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body grid body movement)
                        (move-body body movement))]
        (doseq [component context]
          (context/position-changed component eid))
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

(defmethod tick! :entity/projectile-collision
  [[k {:keys [entity-effects already-hit-bodies piercing?]}]
   eid
   {:keys [cdq.context/grid] :as c}]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (grid/rectangle->cells grid entity)) ; just use cached-touched -cells
        hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                     (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                           (:entity/faction @%))
                                     (:collides? @%)
                                     (entity/collides? entity @%))
                               (grid/cells->entities cells*))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(grid/blocked? % (:z-order entity)) cells*))]
    (when destroy?
      (swap! eid assoc :entity/destroyed? true))
    (when hit-entity
      (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
    (when hit-entity
      (effect-ctx/do-all! c
                          {:effect/source eid
                           :effect/target hit-entity}
                          entity-effects))))

(defmethod tick! :entity/delete-after-animation-stopped?
  [_ eid c]
  (when (animation/stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))

(defmethod tick! :entity/skills
  [[k skills]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (timer/stopped? cooling-down? elapsed-time))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod tick! :entity/string-effect
  [[k {:keys [counter]}]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid dissoc k)))

(defmethod tick! :entity/temp-modifier
  [[k {:keys [modifiers counter]}]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))
