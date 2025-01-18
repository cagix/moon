(ns cdq.world.content-grid
  (:require [cdq.content-grid :as content-grid]
            [cdq.tiled :as tiled]))

(defn create [{:keys [cell-size]}
              {:keys [cdq.context/tiled-map]}]
  (content-grid/create {:cell-size cell-size
                        :width  (tiled/tm-width  tiled-map)
                        :height (tiled/tm-height tiled-map)}))
