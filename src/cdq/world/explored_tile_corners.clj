(ns cdq.world.explored-tile-corners
  (:require [cdq.grid2d :as g2d]
            [cdq.tiled :as tiled]))

(defn create [{:keys [cdq.context/tiled-map]}]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))
