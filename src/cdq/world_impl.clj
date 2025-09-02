(ns cdq.world-impl
  (:require [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.raycaster :as raycaster]
            [cdq.utils :as utils]
            [cdq.world.content-grid :as content-grid]
            [qrecord.core :as q]))

(q/defrecord World [world/tiled-map
                    world/start-position
                    world/grid
                    world/explored-tile-corners
                    world/content-grid
                    world/raycaster
                    world/potential-field-cache
                    world/factions-iterations
                    world/id-counter
                    world/entity-ids
                    world/elapsed-time
                    world/max-delta
                    world/max-speed
                    world/minimum-size
                    world/z-orders
                    world/render-z-order

                    ; added later
                    world/delta-time
                    world/paused?
                    world/active-entities])

(defn- create-explored-tile-corners [tiled-map]
  (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                         (:tiled-map/height tiled-map)
                         (constantly false))))

(defn create
  [{:keys [tiled-map
           start-position]
    :as config}]
  (let [grid (grid-impl/create tiled-map)
        z-orders [:z-order/on-ground
                  :z-order/ground
                  :z-order/flying
                  :z-order/effect]
        ; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
        max-delta 0.04
        ; setting a min-size for colliding bodies so movement can set a max-speed for not
        ; skipping bodies at too fast movement
        ; TODO assert at properties load
        minimum-size 0.39 ; == spider smallest creature size.
        ; set max speed so small entities are not skipped by projectiles
        ; could set faster than max-speed if I just do multiple smaller movement steps in one frame
        max-speed (/ minimum-size max-delta)]
    (merge (map->World {})
           {:world/tiled-map tiled-map
            :world/start-position start-position
            :world/grid grid
            :world/explored-tile-corners (create-explored-tile-corners tiled-map)
            :world/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                     (:tiled-map/height tiled-map)
                                                     (:content-grid-cell-size config))
            :world/raycaster (raycaster/create grid)
            :world/potential-field-cache (atom nil)
            :world/factions-iterations (:potential-field-factions-iterations config)
            :world/id-counter (atom 0)
            :world/entity-ids (atom {})
            :world/elapsed-time 0
            :world/max-delta max-delta
            :world/max-speed max-speed
            :world/minimum-size minimum-size
            :world/z-orders z-orders
            :world/render-z-order (utils/define-order z-orders)})))
