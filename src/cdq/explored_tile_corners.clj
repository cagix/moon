(ns cdq.explored-tile-corners
  (:require [cdq.grid2d :as g2d]
            [gdl.tiled :as tiled]))

(defn create [{:keys [ctx/tiled-map]}]
  (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                         (tiled/tm-height tiled-map)
                         (constantly false))))
