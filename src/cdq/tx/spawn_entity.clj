(ns cdq.tx.spawn-entity
  (:require [cdq.animation :as animation]
            [cdq.body :as body]
            [cdq.creature :as creature]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.entity.stats]
            [cdq.inventory :as inventory]
            [cdq.gdx.math.geom :as geom]
            [cdq.malli :as m]
            [cdq.stats :as modifiers]
            [cdq.timer :as timer]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [clojure.grid2d :as g2d]
            [clojure.math.vector2 :as v]
            [qrecord.core :as q]))

(defrecord Animation [frames frame-duration looping? cnt maxcnt]
  cdq.animation/Animation
  (tick [this delta]
    (let [maxcnt (float maxcnt)
          newcnt (+ (float cnt) (float delta))]
      (assoc this :cnt (cond (< newcnt maxcnt) newcnt
                             looping? (min maxcnt (- newcnt maxcnt))
                             :else maxcnt))))

  (restart [this]
    (assoc this :cnt 0))

  (stopped? [_]
    (and (not looping?) (>= cnt maxcnt)))

  (current-frame [this]
    (frames (min (int (/ (float cnt) (float frame-duration)))
                 (dec (count frames))))))

(defn- create-animation
  [{:keys [animation/frames
           animation/frame-duration
           animation/looping?]}
   _ctx]
  (map->Animation
   {:frames (vec frames)
    :frame-duration frame-duration
    :looping? looping?
    :cnt 0
    :maxcnt (* (count frames) (float frame-duration))}))

(q/defrecord Body [body/position
                   body/width
                   body/height
                   body/collides?
                   body/z-order
                   body/rotation-angle]
  cdq.body/Body
  (overlaps? [body other-body]
    (geom/overlaps? (geom/body->gdx-rectangle body)
                    (geom/body->gdx-rectangle other-body)))

  (touched-tiles [body]
    (geom/body->touched-tiles body))

  (distance [body other-body]
    (v/distance (:body/position body)
                (:body/position other-body))))

(defn- create-body
  [{[x y] :position
    :keys [position
           width
           height
           collides?
           z-order
           rotation-angle]}
   {:keys [ctx/world]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? (:world/minimum-size world) 0)))
  (assert (>= height (if collides? (:world/minimum-size world) 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set (:world/z-orders world)) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :width  (float width)
    :height (float height)
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(def ^:private create-fns {:entity/animation create-animation
                           :entity/body      create-body
                           :entity/delete-after-duration (fn [duration {:keys [ctx/world]}]
                                                           (timer/create (:world/elapsed-time world) duration))
                           :entity/projectile-collision (fn [v _ctx]
                                                          (assoc v :already-hit-bodies #{}))
                           :creature/stats cdq.entity.stats/create})

(defn- create-component [[k v] ctx]
  (if-let [f (create-fns k)]
    (f v ctx)
    v))

(defn- create-fsm
  [{:keys [fsm initial-state]}
   eid
   {:keys [ctx/world]}]
  ; fsm throws when initial-state is not part of states, so no need to assert initial-state
  ; initial state is nil, so associng it. make bug report at reduce-fsm?
  [[:tx/assoc eid :entity/fsm (assoc ((get (:world/fsms world) fsm) initial-state nil) :state initial-state)]
   [:tx/assoc eid initial-state (state/create [initial-state nil] eid world)]])

(defn- create-inventory []
  (->> inventory/empty-inventory
       (map (fn [[slot [width height]]]
              [slot (g2d/create-grid width height (constantly nil))]))
       (into {})))

(defn- create!-inventory [items eid _ctx]
  (cons [:tx/assoc eid :entity/inventory (create-inventory)]
        (for [item items]
          [:tx/pickup-item eid item])))

(def ^:private create!-fns
  {:entity/fsm                             create-fsm
   :entity/inventory                       create!-inventory
   :entity/delete-after-animation-stopped? (fn [_ eid _ctx]
                                             (-> @eid :entity/animation :looping? not assert)
                                             nil)
   :entity/skills                          (fn [skills eid _ctx]
                                             (cons [:tx/assoc eid :entity/skills nil]
                                                   (for [skill skills]
                                                     [:tx/add-skill eid skill])))})

(defn- after-create-component [[k v] eid ctx]
  (when-let [f (create!-fns k)]
    (f v eid ctx)))

(q/defrecord Entity [entity/body]
  entity/Entity
  (position [_]
    (:body/position body))

  (distance [_ other-entity]
    (body/distance body
                   (:entity/body other-entity))))

(extend-type Entity
  creature/Skills
  (skill-usable-state [entity
                       {:keys [skill/cooling-down? skill/effects] :as skill}
                       effect-ctx]
    (cond
     cooling-down?
     :cooldown

     (modifiers/not-enough-mana? (:creature/stats entity) skill)
     :not-enough-mana

     (not (effect/some-applicable? effect-ctx effects))
     :invalid-params

     :else
     :usable)))

(defn do!
  [{:keys [ctx/world]
    :as ctx}
   entity]
  (let [{:keys [world/content-grid
                world/entity-ids
                world/grid
                world/id-counter
                world/spawn-entity-schema
                ]} world
        _ (m/validate-humanize spawn-entity-schema entity)
        entity (reduce (fn [m [k v]]
                         (assoc m k (create-component [k v] ctx)))
                       {}
                       entity)
        _ (assert (and (not (contains? entity :entity/id))))
        entity (assoc entity :entity/id (swap! id-counter inc))
        entity (merge (map->Entity {}) entity)
        eid (atom entity)]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid))
    (grid/set-touched-cells! grid eid)
    (when (:body/collides? (:entity/body @eid))
      (grid/set-occupied-cells! grid eid))
    (mapcat #(after-create-component % eid ctx) @eid)))
