(ns cdq.create.explored-tile-corners
  (:require [clojure.data.grid2d :as g2d]
            [clojure.gdx.tiled :as tiled]))

(defn create [tiled-map]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))
