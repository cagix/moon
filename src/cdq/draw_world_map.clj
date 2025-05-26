(ns cdq.draw-world-map
  (:require [cdq.g :as g]
            [cdq.tile-color-setter :as tile-color-setter]
            [gdl.application]
            [gdl.graphics :as graphics]))

(extend-type gdl.application.Context
  g/DrawWorldMap
  (draw-world-map! [{:keys [ctx/tiled-map
                            ctx/raycaster
                            ctx/explored-tile-corners]
                     :as ctx}]
    (g/draw-tiled-map! ctx
                       tiled-map
                       (tile-color-setter/create
                        {:raycaster raycaster
                         :explored-tile-corners explored-tile-corners
                         :light-position (g/camera-position ctx)
                         :explored-tile-color (graphics/color 0.5 0.5 0.5 1)
                         :see-all-tiles? false}))))
