(ns cdq.world.content-grid
  (:require [clojure.content-grid :as content-grid]
            [clojure.tiled :as tiled]))

(defn create [{:keys [cell-size]}
              {:keys [clojure.context/tiled-map]}]
  (content-grid/create {:cell-size cell-size
                        :width  (tiled/tm-width  tiled-map)
                        :height (tiled/tm-height tiled-map)}))
