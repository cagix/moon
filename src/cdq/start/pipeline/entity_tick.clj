(ns cdq.start.pipeline.entity-tick
  (:require [cdq.animation :as animation]
            [cdq.entity-tick]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.body :as body]
            [cdq.grid :as grid]
            [cdq.grid.cell :as cell]
            [cdq.raycaster :as raycaster]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.timer :as timer]
            [cdq.skill :as skill]
            [cdq.stats :as stats]
            [cdq.gdx.math.vector2 :as v]
            [cdq.utils :as utils]))

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

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [raycaster {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (raycaster/line-of-sight? raycaster @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn- npc-effect-ctx
  [{:keys [ctx/raycaster
           ctx/grid]}
   eid]
  (let [entity @eid
        target (grid/nearest-enemy grid entity)
        target (when (and target
                          (raycaster/line-of-sight? raycaster entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))

(defn- npc-choose-skill [ctx entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % effect-ctx))
                     (effect/applicable-and-useful? ctx effect-ctx (:skill/effects %))))
       first))

(def entity->tick
  {:entity/alert-friendlies-after-duration (fn [{:keys [counter faction]}
                                                eid
                                                {:keys [ctx/elapsed-time
                                                        ctx/grid]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               (cons [:tx/mark-destroyed eid]
                                                     (for [friendly-eid (->> {:position (entity/position @eid)
                                                                              :radius 4}
                                                                             (grid/circle->entities grid)
                                                                             (filter #(= (:entity/faction @%) faction)))]
                                                       [:tx/event friendly-eid :alert]))))
   :entity/animation (fn [animation eid {:keys [ctx/delta-time]}]
                       [[:tx/assoc eid :entity/animation (animation/tick animation delta-time)]])
   :entity/delete-after-animation-stopped? (fn [_ eid _ctx]
                                             (when (animation/stopped? (:entity/animation @eid))
                                               [[:tx/mark-destroyed eid]]))
   :entity/delete-after-duration (fn [counter eid {:keys [ctx/elapsed-time]}]
                                   (when (timer/stopped? elapsed-time counter)
                                     [[:tx/mark-destroyed eid]]))
   :entity/movement (fn [{:keys [direction
                                 speed
                                 rotate-in-movement-direction?]
                          :as movement}
                         eid
                         {:keys [ctx/delta-time
                                 ctx/grid
                                 ctx/max-speed]}]
                      (assert (<= 0 speed max-speed)
                              (pr-str speed))
                      (assert (or (zero? (v/length direction))
                                  (v/nearly-normalised? direction))
                              (str "cannot understand direction: " (pr-str direction)))
                      (when-not (or (zero? (v/length direction))
                                    (nil? speed)
                                    (zero? speed))
                        (let [movement (assoc movement :delta-time delta-time)
                              body (:entity/body @eid)]
                          (when-let [body (if (:body/collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                                            (try-move-solid-body grid body (:entity/id @eid) movement)
                                            (move-body body movement))]
                            [[:tx/move-entity eid body direction rotate-in-movement-direction?]]))))
   :entity/projectile-collision (fn [{:keys [entity-effects already-hit-bodies piercing?]}
                                     eid
                                     {:keys [ctx/grid]}]
                                  ; TODO this could be called from body on collision
                                  ; for non-solid
                                  ; means non colliding with other entities
                                  ; but still collding with other stuff here ? o.o
                                  (let [entity @eid
                                        cells* (map deref (grid/body->cells grid (:entity/body entity))) ; just use cached-touched -cells
                                        hit-entity (utils/find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                                                           (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                                                                 (:entity/faction @%))
                                                                           (:body/collides? (:entity/body @%))
                                                                           (body/overlaps? (:entity/body entity)
                                                                                           (:entity/body @%)))
                                                                     (grid/cells->entities grid cells*))
                                        destroy? (or (and hit-entity (not piercing?))
                                                     (some #(cell/blocked? % (:body/z-order (:entity/body entity))) cells*))]
                                    [(when destroy?
                                       [:tx/mark-destroyed eid])
                                     (when hit-entity
                                       [:tx/assoc-in eid [:entity/projectile-collision :already-hit-bodies] (conj already-hit-bodies hit-entity)] ; this is only necessary in case of not piercing ...
                                       )
                                     (when hit-entity
                                       [:tx/effect {:effect/source eid :effect/target hit-entity} entity-effects])]))
   :entity/skills (fn [skills eid {:keys [ctx/elapsed-time]}]
                    (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
                          :when (and cooling-down?
                                     (timer/stopped? elapsed-time cooling-down?))]
                      [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] false]))
   :active-skill (fn [{:keys [skill effect-ctx counter]}
                      eid
                      {:keys [ctx/elapsed-time
                              ctx/raycaster]}]
                   (cond
                    (not (effect/some-applicable? (update-effect-ctx raycaster effect-ctx) ; TODO how 2 test
                                                  (:skill/effects skill)))
                    [[:tx/event eid :action-done]
                     ; TODO some sound ?
                     ]

                    (timer/stopped? elapsed-time counter)
                    [[:tx/effect effect-ctx (:skill/effects skill)]
                     [:tx/event eid :action-done]]))
   :npc-idle (fn [_ eid {:keys [ctx/grid] :as ctx}]
               (let [effect-ctx (npc-effect-ctx ctx eid)]
                 (if-let [skill (npc-choose-skill ctx @eid effect-ctx)]
                   [[:tx/event eid :start-action [skill effect-ctx]]]
                   [[:tx/event eid :movement-direction (or (potential-fields.movement/find-direction grid eid)
                                                           [0 0])]])))
   :npc-moving (fn [{:keys [timer]} eid {:keys [ctx/elapsed-time]}]
                 (when (timer/stopped? elapsed-time timer)
                   [[:tx/event eid :timer-finished]]))
   :npc-sleeping (fn [_ eid {:keys [ctx/grid]}]
                   (let [entity @eid]
                     (when-let [distance (grid/nearest-enemy-distance grid entity)]
                       (when (<= distance (stats/get-stat-value (:creature/stats entity) :entity/aggro-range))
                         [[:tx/event eid :alert]]))))
   :stunned (fn [{:keys [counter]} eid {:keys [ctx/elapsed-time]}]
              (when (timer/stopped? elapsed-time counter)
                [[:tx/event eid :effect-wears-off]]))
   :entity/string-effect (fn [{:keys [counter]} eid {:keys [ctx/elapsed-time]}]
                           (when (timer/stopped? elapsed-time counter)
                             [[:tx/dissoc eid :entity/string-effect]]))
   :entity/temp-modifier (fn [{:keys [modifiers counter]}
                              eid
                              {:keys [ctx/elapsed-time]}]
                           (when (timer/stopped? elapsed-time counter)
                             [[:tx/dissoc eid :entity/temp-modifier]
                              [:tx/mod-remove eid modifiers]]))})

(defn bind-root [ctx]
  (.bindRoot #'cdq.entity-tick/entity->tick entity->tick)
  ctx)
