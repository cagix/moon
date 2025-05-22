(ns cdq.render.draw-tiled-map
  (:require [cdq.tile-color-setter :as tile-color-setter]
            [gdl.graphics.camera :as camera]
            [gdl.tiled :as tiled]))

(defn do! [{:keys [ctx/tiled-map-renderer
                   ctx/tiled-map
                   ctx/raycaster
                   ctx/explored-tile-corners
                   ctx/world-viewport]}]
  (tiled/draw! (tiled-map-renderer tiled-map)
               tiled-map
               (tile-color-setter/create raycaster
                                         explored-tile-corners
                                         (camera/position (:camera world-viewport)))
               (:camera world-viewport))
  nil)
