(ns cdq.explored-tile-corners
  (:require [gdl.tiled :as tiled]))

(defn create [{:keys [ctx/tiled-map]}]
  (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                         (tiled/tm-height tiled-map)
                         (constantly false))))
