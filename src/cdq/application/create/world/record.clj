(ns cdq.application.create.world.record
  (:require [cdq.application.create.world.info]
            [cdq.entity.state :as state]
            [cdq.impl.content-grid]
            [cdq.impl.grid]
            [cdq.world.grid.cell :as cell]
            [cdq.world :as world]
            [gdl.math.vector2 :as v]
            [gdl.grid2d :as g2d]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [com.badlogic.gdx.maps.tiled :as tiled]
            [reduce-fsm :as fsm])
  (:import (gdl.math RayCaster)))

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
  world/Update
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
