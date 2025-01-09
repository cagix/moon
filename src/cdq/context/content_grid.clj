(ns cdq.context.content-grid
  (:require [gdl.tiled :as tiled]
            [cdq.content-grid :as content-grid]))

(defn create [tiled-map {:keys [cell-size]}]
  (content-grid/create {:cell-size cell-size
                        :width  (tiled/tm-width  tiled-map)
                        :height (tiled/tm-height tiled-map)}))
