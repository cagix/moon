(ns forge.entity.impl
  (:require [forge.entity :as entity]
            [forge.entity.components :refer [add-skill collides? remove-mods event ]]
            [forge.entity.inventory :as inventory]
            [forge.item :as item]
            [forge.world :as world :refer [audiovisual timer stopped?]]
            [malli.core :as m]
            [reduce-fsm :as fsm]))

(defmethod entity/->v :entity/hp   [[_ v]] [v v])
(defmethod entity/->v :entity/mana [[_ v]] [v v])

(defmethod entity/tick :entity/temp-modifier [[k {:keys [modifiers counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)
    (swap! eid remove-mods modifiers)))

(defmethod entity/tick :entity/string-effect [[k {:keys [counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)))

(defmethod entity/destroy :entity/destroy-audiovisual [[_ audiovisuals-id] eid]
  (audiovisual (:position @eid) (build audiovisuals-id)))

(def ^:private shout-radius 4)

(defn- friendlies-in-radius [position faction]
  (->> {:position position
        :radius shout-radius}
       world/circle->entities
       (filter #(= (:entity/faction @%) faction))))

(defmethod entity/tick :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}] eid]
  (when (stopped? counter)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius (:position @eid) faction)]
      (event friendly-eid :alert))))

(defmethods :entity/delete-after-duration
  (entity/->v [duration]
    (timer duration))

  (entity/tick [counter eid]
    (when (stopped? counter)
      (swap! eid assoc :entity/destroyed? true))))

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

; fsm throws when initial-state is not part of states, so no need to assert initial-state
; initial state is nil, so associng it. make bug report at reduce-fsm?
(defn- ->init-fsm [fsm initial-state]
  (assoc (fsm initial-state nil) :state initial-state))

(defmethod entity/create :entity/fsm [[k {:keys [fsm initial-state]}] eid]
  (swap! eid assoc
         k (->init-fsm (case fsm
                         :fsms/player player-fsm
                         :fsms/npc npc-fsm)
                       initial-state)
         initial-state (entity/->v [initial-state eid])))

(defmethods :entity/projectile-collision
  (entity/->v [[_ v]]
    (assoc v :already-hit-bodies #{}))

  (entity/tick [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid]
    ; TODO this could be called from body on collision
    ; for non-solid
    ; means non colliding with other entities
    ; but still collding with other stuff here ? o.o
    (let [entity @eid
          cells* (map deref (world/rectangle->cells entity)) ; just use cached-touched -cells
          hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                       (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                             (:entity/faction @%))
                                       (:collides? @%)
                                       (collides? entity @%))
                                 (world/cells->entities cells*))
          destroy? (or (and hit-entity (not piercing?))
                       (some #(world/blocked? % (:z-order entity)) cells*))]
      (when destroy?
        (swap! eid assoc :entity/destroyed? true))
      (when hit-entity
        (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
      (when hit-entity
        (effects-do! {:effect/source eid :effect/target hit-entity} entity-effects)))))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [{:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (world/rectangle->cells body))]
    (and (not-any? #(world/blocked? % z-order) cells*)
         (->> cells*
              world/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (collides? other-entity body)))))))))

(defn- try-move [body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move body movement)
        (try-move body (assoc movement :direction [xdir 0]))
        (try-move body (assoc movement :direction [0 ydir])))))

(defmethod entity/tick :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
   eid]
  (assert (m/validate world/speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v-length direction))
              (v-normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v-length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time world/delta)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body body movement)
                        (move-body body movement))]
        (world/position-changed eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v-angle-from-vector direction)))))))

(defmethods :entity/skills
  (entity/create [[k skills] eid]
    (swap! eid assoc k nil)
    (doseq [skill skills]
      (swap! eid add-skill skill)))

  (entity/tick [[k skills] eid]
    (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
            :when (and cooling-down?
                       (stopped? cooling-down?))]
      (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false))))

(defmethod entity/create :entity/inventory [[k items] eid]
  (swap! eid assoc k item/empty-inventory)
  (doseq [item items]
    (inventory/pickup-item eid item)))
