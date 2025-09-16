(ns cdq.world.resettable
  (:require [cdq.world.content-grid :as content-grid]
            [cdq.world.explored-tile-corners :as explored-tile-corners]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [clojure.tiled :as tiled]))

(defn reset-state
  [world
   {:keys [tiled-map
           start-position]}]
  (let [width  (:tiled-map/width  tiled-map)
        height (:tiled-map/height tiled-map)
        grid (grid/create width height
                          #(case (tiled/movement-property tiled-map %)
                             "none" :none
                             "air"  :air
                             "all"  :all))]
    (assoc world
           :world/tiled-map tiled-map
           :world/start-position start-position
           :world/grid grid
           :world/content-grid (content-grid/create width height (:content-grid-cell-size world))
           :world/explored-tile-corners (explored-tile-corners/create width height)
           :world/raycaster (raycaster/create grid)
           :world/elapsed-time 0
           :world/potential-field-cache (atom nil)
           :world/id-counter (atom 0)
           :world/entity-ids (atom {})
           :world/paused? false
           :world/mouseover-eid nil)))
