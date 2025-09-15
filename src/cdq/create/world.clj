(ns cdq.create.world
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]
            [cdq.malli :as m]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.explored-tile-corners :as explored-tile-corners]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.utils :as utils]))

(def ^:private components-schema
  (m/schema [:map {:closed true}
             [:entity/body :some]
             [:entity/image {:optional true} :some]
             [:entity/animation {:optional true} :some]
             [:entity/delete-after-animation-stopped? {:optional true} :some]
             [:entity/alert-friendlies-after-duration {:optional true} :some]
             [:entity/line-render {:optional true} :some]
             [:entity/delete-after-duration {:optional true} :some]
             [:entity/destroy-audiovisual {:optional true} :some]
             [:entity/fsm {:optional true} :some]
             [:entity/player? {:optional true} :some]
             [:entity/free-skill-points {:optional true} :some]
             [:entity/click-distance-tiles {:optional true} :some]
             [:entity/clickable {:optional true} :some]
             [:property/id {:optional true} :some]
             [:property/pretty-name {:optional true} :some]
             [:creature/level {:optional true} :some]
             [:entity/faction {:optional true} :some]
             [:entity/species {:optional true} :some]
             [:entity/movement {:optional true} :some]
             [:entity/skills {:optional true} :some]
             [:creature/stats {:optional true} :some]
             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))

(defn- call-world-fn
  [[f params] creature-properties graphics]
  (f
   (assoc params
          :creature-properties creature-properties
          :graphics graphics)))

; TODO make schema
; later added: world/delta-time, active-entities, ?
(defn- create-tiled-map
  [{:keys [ctx/config
           ctx/db
           ctx/graphics]
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
    (assoc ctx :ctx/world {:world/tiled-map tiled-map
                           :world/start-position start-position
                           :world/grid grid
                           :world/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                                    (:tiled-map/height tiled-map)
                                                                    (:content-grid-cell-size (:world config)))
                           :world/explored-tile-corners (explored-tile-corners/create (:tiled-map/width  tiled-map)
                                                                                      (:tiled-map/height tiled-map))
                           :world/raycaster (raycaster/create grid)
                           :world/elapsed-time 0
                           :world/max-delta    (:world/max-delta    (:world config))
                           :world/minimum-size (:world/minimum-size (:world config))
                           :world/z-orders     (:world/z-orders     (:world config))
                           :world/max-speed (/ (:world/minimum-size (:world config))
                                               (:world/max-delta    (:world config)))
                           :world/potential-field-cache (atom nil)
                           :world/factions-iterations (:potential-field-factions-iterations (:world config))
                           :world/id-counter (atom 0)
                           :world/entity-ids (atom {})
                           :world/render-z-order (utils/define-order (:world/z-orders (:world config)))
                           :world/spawn-entity-schema components-schema
                           })))

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
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc-in ctx [:ctx/world :world/player-eid] eid)))

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
  ([ctx world-fn]
   (-> ctx
       (create-tiled-map world-fn)
       spawn-player!
       spawn-enemies!)))
