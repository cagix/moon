(ns cdq.graphics.tiled-map
  (:require [gdl.context :refer [draw-tiled-map]]
            [gdl.graphics.camera :as cam]
            [cdq.graphics.tiled-map.tile-color-setter :as tile-color-setter]))

(defn render [{:keys [gdl.context/world-viewport
                      cdq.context/tiled-map
                      cdq.context/raycaster
                      cdq.context/explored-tile-corners]
               :as context}]
  (draw-tiled-map context
                  tiled-map
                  (tile-color-setter/create raycaster
                                            explored-tile-corners
                                            (cam/position (:camera world-viewport))))
  context)
