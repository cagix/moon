(ns cdq.render.draw-world-map
  (:require [cdq.raycaster :as raycaster]
            [cdq.tile-color-setter :as tile-color-setter]
            [gdl.graphics :as g]
            [gdl.graphics.camera :as camera]))

(defn do! [{:keys [ctx/graphics
                   ctx/world
                   ctx/explored-tile-corners
                   ctx/raycaster]
            :as ctx}]
  (g/draw-tiled-map! graphics
                     (:world/tiled-map world)
                     (tile-color-setter/create
                      {:ray-blocked? (fn [start end] (raycaster/blocked? raycaster start end))
                       :explored-tile-corners explored-tile-corners
                       :light-position (camera/position (:camera (:world-viewport graphics)))
                       :see-all-tiles? false
                       :explored-tile-color  [0.5 0.5 0.5 1]
                       :visible-tile-color   [1 1 1 1]
                       :invisible-tile-color [0 0 0 1]}))
  ctx)
