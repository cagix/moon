(ns cdq.render.draw-world-map
  (:require [cdq.graphics :as graphics]
            [cdq.tile-color-setter :as tile-color-setter]
            [gdl.graphics.color :as color]))

(defn do! [{:keys [ctx/graphics
                   ctx/tiled-map
                   ctx/raycaster
                   ctx/explored-tile-corners]
            :as ctx}]
  (graphics/draw-tiled-map! graphics
                            tiled-map
                            (tile-color-setter/create
                             {:raycaster raycaster
                              :explored-tile-corners explored-tile-corners
                              :light-position (graphics/camera-position graphics)
                              :explored-tile-color (color/create 0.5 0.5 0.5 1)
                              :see-all-tiles? false}))
  ctx)
