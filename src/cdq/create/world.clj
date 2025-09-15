(ns cdq.create.world
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.explored-tile-corners :as explored-tile-corners]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [clojure.tiled :as tiled]
            [clojure.utils :as utils]))

; TODO schema
; world/paused?
; etc.  ? added later, delta-time ?
; player-eid ?

(defn- call-world-fn
  [[f params] creature-properties graphics]
  (f
   (assoc params
          :creature-properties creature-properties
          :graphics graphics)))

(defn- create-tiled-map
  [{:keys [ctx/db
           ctx/graphics
           ctx/world]
    :as ctx}
   world-fn]
  (let [{:keys [tiled-map
                start-position]} (call-world-fn world-fn
                                                (db/all-raw db :properties/creatures)
                                                graphics)
        grid (grid/create (:tiled-map/width  tiled-map)
                          (:tiled-map/height tiled-map)
                          #(case (tiled/movement-property tiled-map %)
                             "none" :none
                             "air"  :air
                             "all"  :all))]
    (update ctx :ctx/world assoc
            :world/tiled-map tiled-map
            :world/start-position start-position
            :world/grid grid
            :world/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                     (:tiled-map/height tiled-map)
                                                     (:content-grid-cell-size world))
            :world/explored-tile-corners (explored-tile-corners/create (:tiled-map/width  tiled-map)
                                                                       (:tiled-map/height tiled-map))
            :world/raycaster (raycaster/create grid)
            :world/elapsed-time 0
            :world/potential-field-cache (atom nil)
            :world/id-counter (atom 0)
            :world/entity-ids (atom {})
            :world/paused? false
            :world/mouseover-eid nil)))

(defn- spawn-player!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:world/player-components world)]
                                          {:position (utils/tile->middle (:world/start-position world))
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc-in ctx [:ctx/world :world/player-eid] eid)))

(defn- spawn-enemies!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world) "creatures" "id")]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature {:position (utils/tile->middle position)
                                           :creature-property (db/build db (keyword creature-id))
                                           :components (:world/enemy-components world)}]]))
  ctx)

(defn do!
  [ctx world-fn]
  (-> ctx
      (create-tiled-map world-fn)
      spawn-player!
      spawn-enemies!))
