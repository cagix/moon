(ns cdq.application.create.content-grid
  (:require [cdq.grid2d :as g2d]
            [cdq.tiled :as tiled]))

(defn- create* [{:keys [cell-size width height]}]
  {:grid (g2d/create-grid
          (inc (int (/ width  cell-size))) ; inc because corners
          (inc (int (/ height cell-size)))
          (fn [idx]
            (atom {:idx idx,
                   :entities #{}})))
   :cell-w cell-size
   :cell-h cell-size})

(defn create [{:keys [cdq.context/tiled-map]}]
  (create* {:cell-size 16
            :width  (tiled/tm-width  tiled-map)
            :height (tiled/tm-height tiled-map)}))
