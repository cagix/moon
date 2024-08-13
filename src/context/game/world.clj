(ns context.game.world
  (:require [clj.gdx.graphics.camera :as camera]
            [math.raycaster :as raycaster]
            [math.vector :as v]
            [utils.core :refer [tile->middle]]
            [data.grid2d :as grid2d]
            [api.context :as ctx :refer [explored? transact-all! ray-blocked? content-grid world-grid]]
            [api.disposable :refer [dispose]]
            [api.entity :as entity]
            [api.maps.tiled :as tiled]
            [api.tx :refer [transact!]]
            [api.world.grid :as world-grid]
            [api.world.content-grid :as content-grid]
            [api.world.cell :as cell]
            (context.game.world grid
                                content-grid
                                [potential-fields :as potential-fields]
                                render)
            [mapgen.movement-property :refer (movement-property)]))

(defn- on-screen? [entity* ctx]
  (let [[x y] (entity/position entity*)
        x (float x)
        y (float y)
        [cx cy] (camera/position (ctx/world-camera ctx))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (ctx/world-viewport-width ctx))  2)))
     (<= ydist (inc (/ (float (ctx/world-viewport-height ctx)) 2))))))

; TO math.... // not tested
(defn- create-double-ray-endpositions
  "path-w in tiles."
  [[start-x start-y] [target-x target-y] path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v/direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v/get-normal-vectors v)
        normal1 (v/scale normal1 (/ path-w 2))
        normal2 (v/scale normal2 (/ path-w 2))
        start1  (v/add [start-x  start-y]  normal1)
        start2  (v/add [start-x  start-y]  normal2)
        target1 (v/add [target-x target-y] normal1)
        target2 (v/add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(def ^:private los-checks? true)

(extend-type api.context.Context
  api.context/World
  (update-potential-fields! [ctx entities]
    (potential-fields/update-potential-fields! (ctx/world-grid ctx)
                                               entities))

  (potential-field-follow-to-enemy [ctx entity]
    (potential-fields/potential-field-follow-to-enemy (ctx/world-grid ctx)
                                                      entity))

  (render-map [ctx]
    (context.game.world.render/render-map ctx (camera/position (ctx/world-camera ctx))))

  (line-of-sight? [context source* target*]
    (and (entity/z-order target*)  ; is even an entity which renders something
         (or (not (:entity/player? source*))
             (on-screen? target* context))
         (not (and los-checks?
                   (ray-blocked? context (entity/position source*) (entity/position target*))))))

  (ray-blocked? [{:keys [context.game/world]} start target]
    (let [{:keys [cell-blocked-boolean-array width height]} world]
      (raycaster/ray-blocked? cell-blocked-boolean-array width height start target)))

  (path-blocked? [context start target path-w]
    (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
      (or
       (ray-blocked? context start1 target1)
       (ray-blocked? context start2 target2))))

  ; TODO put tile param
  (explored? [{:keys [context.game/world] :as context} position]
    (get @(:explored-tile-corners world) position))

  (content-grid [{:keys [context.game/world]}]
    (:content-grid world))

  (world-grid [{:keys [context.game/world]}]
    (:grid world)))

(defmethod transact! :tx/add-to-world [[_ entity] ctx]
  (content-grid/update-entity! (content-grid ctx) entity)
  (world-grid/add-entity! (world-grid ctx) entity)
  ctx)

(defmethod transact! :tx/remove-from-world [[_ entity] ctx]
  (content-grid/remove-entity! (content-grid ctx) entity)
  (world-grid/remove-entity! (world-grid ctx) entity)
  ctx)

(defmethod transact! :tx/position-changed [[_ entity] ctx]
  (content-grid/update-entity! (content-grid ctx) entity)
  (world-grid/entity-position-changed! (world-grid ctx) entity)
  ctx)

(defn- set-cell-blocked-boolean-array [arr cell*]
  (let [[x y] (:position cell*)]
    (aset arr x y (boolean (cell/blocked? cell* :z-order/flying)))))

(defn- ->cell-blocked-boolean-array [grid]
  (let [arr (make-array Boolean/TYPE (grid2d/width grid) (grid2d/height grid))]
    (doseq [cell (grid2d/cells grid)]
      (set-cell-blocked-boolean-array arr @cell))
    arr))

(defn- tiled-map->grid [tiled-map]
  (context.game.world.grid/->build (tiled/width  tiled-map)
                                   (tiled/height tiled-map)
                                   (fn [position]
                                     (case (movement-property tiled-map position)
                                       "none" :none
                                       "air"  :air
                                       "all"  :all))))

; TODO make defrecord
(defn- ->world-map [{:keys [tiled-map start-position] :as world-map}]
  (let [grid (tiled-map->grid tiled-map)
        w (grid2d/width  grid)
        h (grid2d/height grid)]
    (merge world-map
           {:width w
            :height h
            :grid grid
            :cell-blocked-boolean-array (->cell-blocked-boolean-array grid)
            :content-grid (context.game.world.content-grid/->build w h 16 16)
            :explored-tile-corners (atom (grid2d/create-grid w h (constantly false)))}))
  ; TODO
  ; (check-not-allowed-diagonals grid)
  )

(def ^:private spawn-enemies? true)

(defn- transact-create-entities-from-tiledmap! [{:keys [context.game/world] :as ctx}]
  (let [tiled-map (:tiled-map world)
        ctx (if spawn-enemies?
              (transact-all! ctx
                             (for [[posi creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                               [:tx.entity/creature
                                (keyword creature-id)
                                #:entity {:position (tile->middle posi)
                                          :state [:state/npc :sleeping]}]))
              ctx)]
    (tiled/remove-layer! tiled-map :creatures)  ; otherwise will be rendered, is visible
    (transact-all! ctx [[:tx.entity/creature
                         :creatures/vampire
                         #:entity {:position (tile->middle (:start-position world))
                                   :state [:state/player :idle]
                                   :player? true
                                   :free-skill-points 3
                                   :clickable {:type :clickable/player}
                                   :click-distance-tiles 1.5}]])))

(defn- add-world-context [ctx tiled-level]
  (when-let [world (:context.game/world ctx)]
    (dispose (:tiled-map world)))
  (-> ctx
      (assoc :context.game/world (->world-map tiled-level))
      transact-create-entities-from-tiledmap!))

(defn- reset-world-context [ctx]
  (assoc ctx :context.game/world (->world-map (select-keys (:context.game/world ctx)
                                                           [:tiled-map
                                                            :start-position]))))

(defn setup-context [ctx mode tiled-level]
  (case mode
    :game-loop/normal (add-world-context ctx tiled-level)
    :game-loop/replay (reset-world-context ctx)))
