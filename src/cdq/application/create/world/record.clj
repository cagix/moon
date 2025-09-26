(ns cdq.application.create.world.record
  (:require [cdq.application.create.world.info]
            [cdq.creature :as creature]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.body :as body]
            [cdq.entity.state :as state]
            [cdq.stats :as stats]
            [cdq.impl.content-grid]
            [cdq.impl.grid]
            [cdq.malli :as m]
            [cdq.potential-fields.movement]
            [cdq.potential-fields.update]
            [cdq.world.grid.cell :as cell]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [gdl.math.vector2 :as v]
            [gdl.grid2d :as g2d]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [com.badlogic.gdx.maps.tiled :as tiled]
            [qrecord.core :as q]
            [reduce-fsm :as fsm])
  (:import (gdl.math RayCaster)))

(def ^:private create-fns
  (update-vals '{:entity/animation             cdq.entity.animation/create
                 :entity/body                  cdq.entity.body/create
                 :entity/delete-after-duration cdq.entity.delete-after-duration/create
                 :entity/projectile-collision  cdq.entity.projectile-collision/create
                 :creature/stats               cdq.entity.stats/create}
               (fn [sym]
                 (let [avar (requiring-resolve sym)]
                   (assert avar sym)
                   avar))))

(defn- create-component [[k v] world]
  (if-let [f (create-fns k)]
    (f v world)
    v))

(def ^:private create!-fns
  (update-vals '{:entity/fsm                             cdq.entity.fsm/create!
                 :entity/inventory                       cdq.entity.inventory/create!
                 :entity/delete-after-animation-stopped? cdq.entity.delete-after-animation-stopped/create!
                 :entity/skills                          cdq.entity.skills/create!}
               (fn [sym]
                 (let [avar (requiring-resolve sym)]
                   (assert avar sym)
                   avar))))

(defn- after-create-component [[k v] eid world]
  (when-let [f (create!-fns k)]
    (f v eid world)))

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

     (stats/not-enough-mana? (:creature/stats entity) skill)
     :not-enough-mana

     (not (seq (filter #(effect/applicable? % effect-ctx) effects)))
     :invalid-params

     :else
     :usable)))

(defn- blocked? [[arr width height] [start-x start-y] [target-x target-y]]
  (RayCaster/rayBlocked (double start-x)
                        (double start-y)
                        (double target-x)
                        (double target-y)
                        width
                        height
                        arr))

(defn- create-double-ray-endpositions
  [[start-x start-y]
   [target-x target-y]
   path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v/direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v/normal-vectors v)
        normal1 (v/scale normal1 (/ path-w 2))
        normal2 (v/scale normal2 (/ path-w 2))
        start1  (v/add [start-x  start-y]  normal1)
        start2  (v/add [start-x  start-y]  normal2)
        target1 (v/add [target-x target-y] normal1)
        target2 (v/add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(defn- path-blocked? [raycaster start target path-w]
  (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
    (or
     (blocked? raycaster start1 target1)
     (blocked? raycaster start2 target2))))

(defn- create-explored-tile-corners [width height]
  (atom (g2d/create-grid width height (constantly false))))

(defn- create-raycaster [g2d]
  (let [width  (g2d/width  g2d)
        height (g2d/height g2d)
        cells  (for [cell (map deref (g2d/cells g2d))]
                 [(:position cell)
                  (boolean (cell/blocks-vision? cell))])]
    (let [arr (make-array Boolean/TYPE width height)]
      (doseq [[[x y] blocked?] cells]
        (aset arr x y (boolean blocked?)))
      [arr width height])))

(defrecord World []
  world/Entities
  (spawn-entity! [{:keys [world/content-grid
                          world/entity-ids
                          world/grid
                          world/id-counter
                          world/spawn-entity-schema]
                   :as world}
                  entity]
    (m/validate-humanize spawn-entity-schema entity)
    (let [entity (reduce (fn [m [k v]]
                           (assoc m k (create-component [k v] world)))
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
      (mapcat #(after-create-component % eid world) @eid)))

  world/Update
  (update-potential-fields! [world]
    (cdq.potential-fields.update/do! world))

  (update-time [{:keys [world/max-delta]
                 :as world}
                delta-ms]
    (let [delta-ms (min delta-ms max-delta)]
      (-> world
          (assoc :world/delta-time delta-ms)
          (update :world/elapsed-time + delta-ms))))

  world/FSMs
  (handle-event [world eid event]
    (world/handle-event world eid event nil))

  (handle-event [world eid event params]
    (let [fsm (:entity/fsm @eid)
          _ (assert fsm)
          old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (let [k (:state (:entity/fsm @eid))]
                              [k (k @eid)])
              new-state-obj [new-state-k (state/create [new-state-k params] eid world)]]
          [[:tx/assoc       eid :entity/fsm new-fsm]
           [:tx/assoc       eid new-state-k (new-state-obj 1)]
           [:tx/dissoc      eid old-state-k]
           [:tx/state-exit  eid old-state-obj]
           [:tx/state-enter eid new-state-obj]]))))

  world/InfoText
  (info-text [world entity]
    (cdq.application.create.world.info/info-text world entity))

  world/RayCaster
  (ray-blocked? [{:keys [world/raycaster]} start target]
    (blocked? raycaster start target))

  (path-blocked? [{:keys [world/raycaster]} start target path-w]
    (path-blocked? raycaster start target path-w))

  (line-of-sight? [{:keys [world/raycaster]} source target]
    (not (blocked? raycaster
                   (:body/position (:entity/body source))
                   (:body/position (:entity/body target)))))

  world/MovementAI
  (find-movement-direction [{:keys [world/grid]} eid]
    (cdq.potential-fields.movement/find-movement-direction grid eid))

  disposable/Disposable
  (dispose! [{:keys [world/tiled-map]}]
    (when tiled-map ; initialization
      (disposable/dispose! tiled-map)))

  world/World
  (active-eids [this]
    (:world/active-entities this))

  world/Resettable
  (reset-state [world {:keys [tiled-map
                              start-position]}]
    (let [width  (:tiled-map/width  tiled-map)
          height (:tiled-map/height tiled-map)
          grid (cdq.impl.grid/create width height
                                     #(case (tiled/movement-property tiled-map %)
                                        "none" :none
                                        "air"  :air
                                        "all"  :all))]
      (assoc world
             :world/tiled-map tiled-map
             :world/start-position start-position
             :world/grid grid
             :world/content-grid (cdq.impl.content-grid/create width height (:content-grid-cell-size world))
             :world/explored-tile-corners (create-explored-tile-corners width height)
             :world/raycaster (create-raycaster grid)
             :world/elapsed-time 0
             :world/potential-field-cache (atom nil)
             :world/id-counter (atom 0)
             :world/entity-ids (atom {})
             :world/paused? false
             :world/mouseover-eid nil))))

(defn do! [world]
  (merge (map->World {}) world))
