(ns cdq.world.explored-tile-corners
  (:require [clojure.grid2d :as g2d]
            [clojure.tiled :as tiled]))

(defn create [{:keys [clojure.context/tiled-map]}]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))
