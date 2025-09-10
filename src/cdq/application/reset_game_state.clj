(ns cdq.application.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.faction :as faction]
            [cdq.gdx.math.geom :as geom]
            [cdq.grid :as grid]
            [cdq.grid.cell :as cell]
            [cdq.grid2d :as g2d]
            [cdq.math.raycaster :as raycaster]
            [cdq.utils :as utils]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.gdx.utils.disposable :as disposable]))

(defn- grid->raycaster [grid]
  (let [width  (g2d/width  (.g2d grid))
        height (g2d/height (.g2d grid))
        cells  (for [cell (map deref (g2d/cells (.g2d grid)))]
                 [(:position cell)
                  (boolean (cell/blocks-vision? cell))])]
    (raycaster/create width height cells)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- body->occupied-cells [grid {:keys [body/position body/width body/height] :as body}]
  (if (or (> (float width) 1) (> (float height) 1))
    (grid/body->cells grid body)
    [(grid/cell grid (mapv int position))]))

(deftype Grid [g2d]
  grid/Grid
  (cell [_ position]
    (g2d position))

  (cells [_ int-positions]
    (into [] (keep g2d) int-positions))

  (body->cells [this body]
    (grid/cells this (geom/body->touched-tiles body)))

  (circle->cells [this circle]
    (->> circle
         geom/circle->outer-rectangle
         geom/rectangle->touched-tiles
         (grid/cells this)))

  (circle->entities [this {:keys [position radius] :as circle}]
    (->> (grid/circle->cells this circle)
         (map deref)
         (grid/cells->entities this)
         (filter #(geom/overlaps?
                   (geom/circle (position 0) (position 1) radius)
                   (geom/body->gdx-rectangle (:entity/body @%))))))

  (cells->entities [_ cells]
    (into #{} (mapcat :entities) cells))

  (cached-adjacent-cells [this cell]
    (if-let [result (:adjacent-cells @cell)]
      result
      (let [result (->> @cell
                        :position
                        grid/get-8-neighbour-positions
                        (grid/cells this))]
        (swap! cell assoc :adjacent-cells result)
        result)))

  (point->entities [this position]
    (when-let [cell (grid/cell this (mapv int position))]
      (filter #(geom/contains? (geom/body->gdx-rectangle (:entity/body @%)) position)
              (:entities @cell))))

  (set-touched-cells! [grid eid]
    (let [cells (grid/body->cells grid (:entity/body @eid))]
      (assert (not-any? nil? cells))
      (swap! eid assoc ::touched-cells cells)
      (doseq [cell cells]
        (assert (not (get (:entities @cell) eid)))
        (swap! cell update :entities conj eid))))

  (remove-from-touched-cells! [_ eid]
    (doseq [cell (::touched-cells @eid)]
      (assert (get (:entities @cell) eid))
      (swap! cell update :entities disj eid)))

  (set-occupied-cells! [grid eid]
    (let [cells (body->occupied-cells grid (:entity/body @eid))]
      (doseq [cell cells]
        (assert (not (get (:occupied @cell) eid)))
        (swap! cell update :occupied conj eid))
      (swap! eid assoc ::occupied-cells cells)))

  (remove-from-occupied-cells! [_ eid]
    (doseq [cell (::occupied-cells @eid)]
      (assert (get (:occupied @cell) eid))
      (swap! cell update :occupied disj eid)))

  (valid-position? [this {:keys [body/z-order] :as body} entity-id]
    {:pre [(:body/collides? body)]}
    (let [cells* (into [] (map deref) (grid/body->cells this body))]
      (and (not-any? #(cell/blocked? % z-order) cells*)
           (->> cells*
                (grid/cells->entities this)
                (not-any? (fn [other-entity]
                            (let [other-entity @other-entity]
                              (and (not= (:entity/id other-entity) entity-id)
                                   (:body/collides? (:entity/body other-entity))
                                   (geom/overlaps? (geom/body->gdx-rectangle (:entity/body other-entity))
                                                   (geom/body->gdx-rectangle body))))))))))

  (nearest-enemy-distance [grid entity]
    (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                  (faction/enemy (:entity/faction entity))))

  (nearest-enemy [grid entity]
    (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                         (faction/enemy (:entity/faction entity)))))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  cell/Cell
  (blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied))

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance))

  (pf-blocked? [this]
    (cell/blocked? this :z-order/ground)))

(defn- create-grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (utils/tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn create-grid [tiled-map]
  (->Grid
   (g2d/create-grid (:tiled-map/width  tiled-map)
                    (:tiled-map/height tiled-map)
                    (fn [position]
                      (atom (create-grid-cell position
                                              (case (tiled/movement-property tiled-map position)
                                                "none" :none
                                                "air"  :air
                                                "all"  :all)))))))

(defn- create-explored-tile-corners [tiled-map]
  (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                         (:tiled-map/height tiled-map)
                         (constantly false))))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:cdq.game/player-props config)]
                                          {:position (utils/tile->middle (:world/start-position world))
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
  ctx)

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world) "creatures" "id")]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature {:position (utils/tile->middle position)
                                           :creature-property (db/build db (keyword creature-id))
                                           :components (:cdq.game/enemy-components config)}]]))
  ctx)

(defn- assoc-player-eid
  [{:keys [ctx/entity-ids]
    :as ctx}]
  (let [eid (get @entity-ids 1)]
    (assert (:entity/player? @eid))
    (assoc ctx :ctx/player-eid eid)))

(defn- create-tiled-map-into-ctx-world
  [{:keys [ctx/db
           ctx/textures]
    :as ctx}
   world-fn]
  (let [{:keys [tiled-map
                start-position]} (let [[f params] world-fn]
                                   ((requiring-resolve f)
                                    (assoc params
                                           :creature-properties (db/all-raw db :properties/creatures)
                                           :textures textures)))]
    (assert tiled-map)
    (assert start-position)
    (when-let [tiled-map (:world/tiled-map (:ctx/world ctx))]
      (disposable/dispose! tiled-map))
    (assoc ctx :ctx/world {:world/tiled-map tiled-map
                           :world/start-position start-position})))

(defn- build-dependent-data
  [{:keys [ctx/config
           ctx/world]
    :as ctx}]
  (merge ctx
         (let [tiled-map (:world/tiled-map world)
               grid (create-grid tiled-map)]
           {:ctx/grid grid
            :ctx/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                   (:tiled-map/height tiled-map)
                                                   (:content-grid-cell-size (:world config)))
            :ctx/explored-tile-corners (create-explored-tile-corners tiled-map)
            :ctx/raycaster (grid->raycaster grid)
            :ctx/elapsed-time 0
            :ctx/max-speed (/ (:ctx/minimum-size ctx)
                              (:ctx/max-delta ctx))
            :ctx/potential-field-cache (atom nil)
            :ctx/factions-iterations (:potential-field-factions-iterations (:world config))
            :ctx/id-counter (atom 0)
            :ctx/entity-ids (atom {})
            :ctx/render-z-order (utils/define-order (:ctx/z-orders ctx))})))

(defn reset-game-state!
  [ctx world-fn]
  (-> ctx
      (create-tiled-map-into-ctx-world world-fn)
      build-dependent-data
      spawn-player!
      assoc-player-eid
      spawn-enemies!))
