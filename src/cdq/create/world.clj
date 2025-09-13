(ns cdq.create.world
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]
            [cdq.world.grid :as grid]
            [clojure.utils :as utils]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.explored-tile-corners :as explored-tile-corners]
            [cdq.world.raycaster :as raycaster]
            [clojure.gdx.maps.tiled :as tiled]))

(defn- call-world-fn
  [[f params] creature-properties graphics]
  (f
   (assoc params
          :creature-properties creature-properties
          :graphics graphics)))

(defn- assoc-ctx-world
  [ctx {:keys [tiled-map
               start-position]}]
  (assoc ctx :ctx/world {:world/tiled-map tiled-map
                         :world/start-position start-position}))

(defn- build-world
  [{:keys [ctx/config
           ctx/world]
    :as ctx}]
  (merge ctx
         (let [tiled-map (:world/tiled-map world)
               grid (grid/create (:tiled-map/width  tiled-map)
                                 (:tiled-map/height tiled-map)
                                 #(case (tiled/movement-property tiled-map %)
                                    "none" :none
                                    "air"  :air
                                    "all"  :all))]
           {:ctx/grid grid
            :ctx/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                   (:tiled-map/height tiled-map)
                                                   (:content-grid-cell-size (:world config)))
            :ctx/explored-tile-corners (explored-tile-corners/create (:tiled-map/width  tiled-map)
                                                                     (:tiled-map/height tiled-map))
            :ctx/raycaster (raycaster/create grid)
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
