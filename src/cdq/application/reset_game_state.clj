(ns cdq.application.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.db :as db]
            [cdq.cell-impl]
            [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.utils :as utils]
            [cdq.raycaster-impl]
            [cdq.world :as world]
            [clojure.gdx.maps.tiled :as tiled]))

(defn create-grid [tiled-map]
  (grid-impl/->Grid
   (g2d/create-grid (:tiled-map/width  tiled-map)
                    (:tiled-map/height tiled-map)
                    (fn [position]
                      (atom (cdq.cell-impl/create position
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
           ctx/db]
    :as ctx}
   start-position]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:cdq.game/player-props config)]
                                          {:position (utils/tile->middle start-position)
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
  ctx)

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/tiled-map]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property tiled-map "creatures" "id")]
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

; TODO dispose old ctx/tiled-map if already present
(defn reset-game-state!
  [{:keys [ctx/config
           ctx/db
           ctx/textures]
    :as ctx}
   world-fn]
  (let [world (let [[f params] world-fn]
                ((requiring-resolve f)
                 (assoc params
                        :creature-properties (db/all-raw db :properties/creatures)
                        :textures textures)))
        {:keys [tiled-map]
         :as world-config} (merge (:world config) world)
        grid (create-grid tiled-map)
        z-orders [:z-order/on-ground
                  :z-order/ground
                  :z-order/flying
                  :z-order/effect]
        max-delta 0.04
        minimum-size 0.39
        max-speed (/ minimum-size max-delta)]
    (-> ctx
        (merge {:ctx/tiled-map (:tiled-map world-config)
                :ctx/grid grid
                :ctx/content-grid (content-grid/create (:tiled-map/width  (:tiled-map world-config))
                                                       (:tiled-map/height (:tiled-map world-config))
                                                       (:content-grid-cell-size world-config))
                :ctx/explored-tile-corners (create-explored-tile-corners (:tiled-map world-config))
                :ctx/raycaster (cdq.raycaster-impl/create grid)
                :ctx/elapsed-time 0
                :ctx/max-delta max-delta
                :ctx/max-speed max-speed
                :ctx/minimum-size minimum-size
                :ctx/z-orders z-orders
                :ctx/potential-field-cache (atom nil)
                :ctx/factions-iterations (:potential-field-factions-iterations world-config)
                :ctx/id-counter (atom 0)
                :ctx/entity-ids (atom {})
                :ctx/render-z-order (utils/define-order z-orders)})
        (spawn-player! (:start-position world-config))
        assoc-player-eid
        spawn-enemies!)))
