(ns cdq.application.create.content-grid
  (:require [cdq.content-grid :as content-grid]
            [cdq.tiled :as tiled]))

(defn create [{:keys [cdq.context/tiled-map]}]
  (content-grid/create {:cell-size 16
                        :width  (tiled/tm-width  tiled-map)
                        :height (tiled/tm-height tiled-map)}))
