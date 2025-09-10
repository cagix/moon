(ns cdq.render.draw-world-map
  (:require [cdq.math.raycaster :as raycaster]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.gdx.graphics :as graphics]))

(defn do!
  [{:keys [ctx/explored-tile-corners
           ctx/raycaster
           ctx/world]
    :as ctx}]
  (graphics/draw-tiled-map! ctx
                            (:world/tiled-map world)
                            (tile-color-setter/create
                             {:ray-blocked? (partial raycaster/blocked? raycaster)
                              :explored-tile-corners explored-tile-corners
                              :light-position (graphics/camera-position graphics)
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]})))
