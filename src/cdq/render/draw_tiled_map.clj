(ns cdq.render.draw-tiled-map
  (:require [cdq.g :as g]
            [cdq.tile-color-setter :as tile-color-setter]))

(defn do! [{:keys [ctx/tiled-map
                   ctx/raycaster
                   ctx/explored-tile-corners]
            :as ctx}]
  (g/draw-tiled-map! ctx
                     tiled-map
                     (tile-color-setter/create raycaster
                                               explored-tile-corners
                                               (g/camera-position ctx)))
  nil)
