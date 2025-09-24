(ns cdq.render.tick-entities
  (:require [cdq.animation :as animation]
            [cdq.body :as body]
            [cdq.creature :as creature]
            [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.stats :as stats]
            [cdq.timer :as timer]
            [cdq.world :as world]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [clojure.grid2d :as g2d]
            [clojure.utils :as utils]
            [clojure.math.vector2 :as v]))

(defn- npc-choose-skill [world entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (creature/skill-usable-state entity % effect-ctx))
                     (->> (:skill/effects %)
                          (filter (fn [e] (effect/applicable? e effect-ctx)))
                          (some (fn [e] (effect/useful? e effect-ctx world))))))
       first))

(defn- npc-effect-ctx
  [{:keys [world/grid]
    :as world}
   eid]
  (let [entity @eid
        target (grid/nearest-enemy grid entity)
        target (when (and target
                          (world/line-of-sight? world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))

(defn- alert-friendlies-after-duration
  [{:keys [counter faction]}
   eid
   {:keys [world/elapsed-time
           world/grid]}]
  (when (timer/stopped? elapsed-time counter)
    (cons [:tx/mark-destroyed eid]
          (for [friendly-eid (->> {:position (entity/position @eid)
                                   :radius 4}
                                  (grid/circle->entities grid)
                                  (filter #(= (:entity/faction @%) faction)))]
            [:tx/event friendly-eid :alert]))))

(defn- update-animation [animation eid {:keys [world/delta-time]}]
  [[:tx/assoc eid :entity/animation (animation/tick animation delta-time)]])

(defn- delete-after-animation-stopped [_ eid _world]
  (when (animation/stopped? (:entity/animation @eid))
    [[:tx/mark-destroyed eid]]))

(defn- delete-after-duration [counter eid {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/mark-destroyed eid]]))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (update body :body/position move-position movement))

(defn- try-move [grid body entity-id movement]
  (let [new-body (move-body body movement)]
    (when (grid/valid-position? grid new-body entity-id)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [grid body entity-id {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move grid body entity-id movement)
        (try-move grid body entity-id (assoc movement :direction [xdir 0]))
        (try-move grid body entity-id (assoc movement :direction [0 ydir])))))

(defn- update-movement
  [{:keys [direction
           speed
           rotate-in-movement-direction?]
    :as movement}
   eid
   {:keys [world/delta-time
           world/grid
           world/max-speed]}]
  (assert (<= 0 speed max-speed)
          (pr-str speed))
  (assert (or (zero? (v/length direction))
              (utils/nearly-equal? 1 (v/length direction)))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v/length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time delta-time)
          body (:entity/body @eid)]
      (when-let [body (if (:body/collides? body)
                        (try-move-solid-body grid body (:entity/id @eid) movement)
                        (move-body body movement))]
        [[:tx/move-entity eid body direction rotate-in-movement-direction?]]))))

(defn- check-projectile-collision
  [{:keys [entity-effects already-hit-bodies piercing?]}
   eid
   {:keys [world/grid]}]
  (let [entity @eid
        cells* (map deref (g2d/get-cells grid (body/touched-tiles (:entity/body entity))))
        hit-entity (first (filter #(and (not (contains? already-hit-bodies %))
                                        (not= (:entity/faction entity)
                                              (:entity/faction @%))
                                        (:body/collides? (:entity/body @%))
                                        (body/overlaps? (:entity/body entity)
                                                        (:entity/body @%)))
                                  (grid/cells->entities grid cells*)))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(cell/blocked? % (:body/z-order (:entity/body entity))) cells*))]
    [(when destroy?
       [:tx/mark-destroyed eid])
     (when hit-entity
       [:tx/assoc-in
        eid
        [:entity/projectile-collision
         :already-hit-bodies]
        (conj already-hit-bodies hit-entity)])
     (when hit-entity
       [:tx/effect
        {:effect/source eid
         :effect/target hit-entity}
        entity-effects])]))

(defn- update-skills-cooldown [skills eid {:keys [world/elapsed-time]}]
  (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
        :when (and cooling-down?
                   (timer/stopped? elapsed-time cooling-down?))]
    [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] false]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [world {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (world/line-of-sight? world @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn- update-active-skill
  [{:keys [skill effect-ctx counter]}
   eid
   {:keys [world/elapsed-time]
    :as world}]
  (let [effect-ctx (update-effect-ctx world effect-ctx)]
    (cond
     (not (seq (filter #(effect/applicable? % effect-ctx)
                       (:skill/effects skill))))
     [[:tx/event eid :action-done]
      ; TODO some sound ?
      ]

     (timer/stopped? elapsed-time counter)
     [[:tx/effect effect-ctx (:skill/effects skill)]
      [:tx/event eid :action-done]])))

(defn- update-npc-idle [_ eid world]
  (let [effect-ctx (npc-effect-ctx world eid)]
    (if-let [skill (npc-choose-skill world @eid effect-ctx)]
      [[:tx/event eid :start-action [skill effect-ctx]]]
      [[:tx/event eid :movement-direction (or (world/find-movement-direction world eid)
                                              [0 0])]])))

(defn- update-npc-moving [{:keys [timer]} eid {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time timer)
    [[:tx/event eid :timer-finished]]))

(defn- update-npc-sleeping [_ eid {:keys [world/grid]}]
  (let [entity @eid]
    (when-let [distance (grid/nearest-enemy-distance grid entity)]
      (when (<= distance (stats/get-stat-value (:creature/stats entity) :entity/aggro-range))
        [[:tx/event eid :alert]]))))

(defn- tick-stunned-timer [{:keys [counter]} eid {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/event eid :effect-wears-off]]))

(defn- tick-string-effect-timer
  [{:keys [counter]}
   eid
   {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc eid :entity/string-effect]]))

(defn- tick-temp-modifier
  [{:keys [modifiers counter]}
   eid
   {:keys [world/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc     eid :entity/temp-modifier]
     [:tx/mod-remove eid modifiers]]))

(def ^:private k->tick-fn
  {:entity/alert-friendlies-after-duration alert-friendlies-after-duration
   :entity/animation update-animation
   :entity/delete-after-animation-stopped? delete-after-animation-stopped
   :entity/delete-after-duration delete-after-duration
   :entity/movement update-movement
   :entity/projectile-collision check-projectile-collision
   :entity/skills update-skills-cooldown
   :active-skill update-active-skill
   :npc-idle update-npc-idle
   :npc-moving update-npc-moving
   :npc-sleeping update-npc-sleeping
   :stunned tick-stunned-timer
   :entity/string-effect tick-string-effect-timer
   :entity/temp-modifier tick-temp-modifier})

(comment
 :tx/assoc
 :tx/assoc-in
 :tx/dissoc
 :tx/mark-destroyed
 :tx/move-entity
 :tx/mod-remove
 :tx/event (comment
            :tx/state-exit
            :tx/state-enter
            )
 :tx/effect (comment
             :tx/audiovisual
             :tx/sound
             :tx/spawn-creature
             :tx/spawn-line
             )
 )

(defn tick-entities!
  [{:keys [ctx/world]
    :as ctx}]
  (doseq [eid (:world/active-entities world)
          [k v] @eid
          :let [f (k->tick-fn k)]
          :when f]
    (ctx/handle-txs! ctx (f v eid world))))
