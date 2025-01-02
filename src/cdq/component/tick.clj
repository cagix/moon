(ns cdq.component.tick
  (:require [anvil.controls :as controls]
            [anvil.effect :as effect]
            [anvil.entity :as entity]
            [anvil.skill :as skill]
            [anvil.world.potential-field :as potential-field]
            [cdq.context :as world :refer [stopped? friendlies-in-radius]]
            [cdq.grid :as grid]
            [clojure.component :as component]
            [clojure.utils :refer [find-first]]
            [gdl.graphics.animation :as animation]
            [gdl.malli :as m]
            [gdl.math.vector :as v]))

(defn- npc-choose-skill [c entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect/applicable-and-useful? c ctx (:skill/effects %))))
       first))

(defn- effect-context [c eid]
  (let [entity @eid
        target (world/nearest-enemy c entity)
        target (when (and target
                          (world/line-of-sight? c entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defmethod component/tick :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}] eid c]
  (when (stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius c (:position @eid) faction)]
      (entity/event c friendly-eid :alert))))

(defmethod component/tick :entity/animation
  [[k animation] eid {:keys [cdq.context/delta-time]}]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation delta-time)))))

(defmethod component/tick :entity/delete-after-animation-stopped?
  [_ eid c]
  (when (animation/stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))

(defmethod component/tick :entity/delete-after-duration
  [[_ counter] eid c]
  (when (stopped? c counter)
    (swap! eid assoc :entity/destroyed? true)))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [c {:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (world/rectangle->cells c body))]
    (and (not-any? #(grid/blocked? % z-order) cells*)
         (->> cells*
              grid/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (entity/collides? other-entity body)))))))))

(defn- try-move [c body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? c new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [c body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move c body movement)
        (try-move c body (assoc movement :direction [xdir 0]))
        (try-move c body (assoc movement :direction [0 ydir])))))

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ world/minimum-size
                            world/max-delta-time)) ; need to make var because m/schema would fail later if divide / is inside the schema-form

(def speed-schema (m/schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod component/tick :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
   eid
   {:keys [cdq.context/delta-time] :as c}]
  (assert (m/validate speed-schema speed)
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
                        (try-move-solid-body c body movement)
                        (move-body body movement))]
        (world/position-changed c eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

(defmethod component/tick :entity/projectile-collision
  [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid c]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (world/rectangle->cells c entity)) ; just use cached-touched -cells
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
      (effect/do-all! c
                      {:effect/source eid
                       :effect/target hit-entity}
                      entity-effects))))

(defmethod component/tick :entity/skills
  [[k skills] eid c]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? c cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod component/tick :entity/string-effect
  [[k {:keys [counter]}] eid c]
  (when (stopped? c counter)
    (swap! eid dissoc k)))

(defmethod component/tick :entity/temp-modifier
  [[k {:keys [modifiers counter]}] eid c]
  (when (stopped? c counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

(defmethod component/tick :stunned
  [[_ {:keys [counter]}] eid c]
  (when (stopped? c counter)
    (entity/event c eid :effect-wears-off)))

(defmethod component/tick :player-moving
  [[_ {:keys [movement-vector]}] eid c]
  (if-let [movement-vector (controls/movement-vector c)]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)})
    (entity/event c eid :no-movement-input)))

(defmethod component/tick :npc-sleeping
  [_ eid c]
  (let [entity @eid
        cell (world/grid-cell c (entity/tile entity))] ; pattern!
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (entity/event c eid :alert)))))

(defmethod component/tick :npc-moving
  [[_ {:keys [counter]}] eid c]
  (when (stopped? c counter)
    (entity/event c eid :timer-finished)))

(defmethod component/tick :npc-idle
  [_ eid c]
  (let [effect-ctx (effect-context c eid)]
    (if-let [skill (npc-choose-skill c @eid effect-ctx)]
      (entity/event c eid :start-action [skill effect-ctx])
      (entity/event c eid :movement-direction (or (potential-field/find-direction c eid) [0 0])))))
