(ns cdq.context.content-grid
  (:require [clojure.gdx.tiled :as tiled]
            [cdq.content-grid :as content-grid]))

(defn create [[_ {:keys [cell-size]}] {:keys [cdq.context/tiled-map]}]
  (content-grid/create {:cell-size cell-size
                        :width  (tiled/tm-width  tiled-map)
                        :height (tiled/tm-height tiled-map)}))
