(ns cdq.create.world
  (:require [cdq.content-grid :as content-grid]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.grid.cell :as cell]
            [cdq.grid2d :as g2d]
            [cdq.math.raycaster :as raycaster]
            [cdq.utils :as utils]
            [clojure.gdx.maps.tiled :as tiled]))

(defn- call-world-fn
  [[f params] creature-properties graphics]
  ((requiring-resolve f)
   (assoc params
          :creature-properties creature-properties
          :graphics graphics)))

(defn- assoc-ctx-world
  [ctx {:keys [tiled-map
               start-position]}]
  (assoc ctx :ctx/world {:world/tiled-map tiled-map
                         :world/start-position start-position}))

(defn- create-explored-tile-corners [tiled-map]
  (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                         (:tiled-map/height tiled-map)
                         (constantly false))))

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

(defn- build-world
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

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/entity-ids
           ctx/world]
    :as ctx}]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:cdq.game/player-props config)]
                                          {:position (utils/tile->middle (:world/start-position world))
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
  (let [eid (get @entity-ids 1)]
    (assert (:entity/player? @eid))
    (assoc ctx :ctx/player-eid eid)))

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

(defn do!
  ([ctx]
   (do! ctx (:starting-world (:cdq.create.world (:ctx/config ctx)))))
  ([{:keys [ctx/db
            ctx/graphics]
     :as ctx}
    world-fn]
   (-> ctx
       (assoc-ctx-world (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics))
       build-world
       spawn-player!
       spawn-enemies!)))
