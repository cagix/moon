(ns cdq.context.explored-tile-corners
  (:require [data.grid2d :as g2d]
            [clojure.gdx.tiled :as tiled]))

(defn create [_ {:keys [cdq.context/tiled-map]}]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))
