(ns cdq.application.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.db :as db]
            [cdq.grid :as grid]
            [cdq.grid.cell :as cell]
            [cdq.grid2d :as g2d]
            [cdq.math.raycaster :as raycaster]
            [cdq.utils :as utils]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.gdx.utils.disposable :as disposable]))

(defn- grid->raycaster [g2d]
  (let [width  (g2d/width  g2d)
        height (g2d/height g2d)
        cells  (for [cell (map deref (g2d/cells g2d))]
                 [(:position cell)
                  (boolean (cell/blocks-vision? cell))])]
    (raycaster/create width height cells)))

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

(defn- create-grid [tiled-map]
  (g2d/create-grid (:tiled-map/width  tiled-map)
                   (:tiled-map/height tiled-map)
                   (fn [position]
                     (atom (create-grid-cell position
                                             (case (tiled/movement-property tiled-map position)
                                               "none" :none
                                               "air"  :air
                                               "all"  :all))))))

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
