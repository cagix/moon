(ns cdq.render.draw-world-map
  (:require [cdq.math.raycaster :as raycaster]
            [cdq.ctx.graphics :as graphics]
            [cdq.render.draw-world-map.tile-color-setter :as tile-color-setter]))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]}]
  (graphics/draw-tiled-map! graphics
                            (:world/tiled-map world)
                            (tile-color-setter/create
                             {:ray-blocked? (partial raycaster/blocked? (:world/raycaster world))
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (graphics/camera-position graphics)
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]})))
