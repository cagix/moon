(ns cdq.render.draw-world-map
  (:require [cdq.raycaster :as raycaster]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.graphics :as g]))

(defn do! [{:keys [ctx/graphics
                   ctx/world]
            :as ctx}]
  (g/draw-tiled-map! graphics
                     (:world/tiled-map world)
                     (tile-color-setter/create
                      {:ray-blocked? (let [raycaster (:world/raycaster world)]
                                       (fn [start end] (raycaster/blocked? raycaster start end)))
                       :explored-tile-corners (:world/explored-tile-corners world)
                       :light-position (:camera/position (:viewport/camera (:world-viewport graphics)))
                       :see-all-tiles? false
                       :explored-tile-color  [0.5 0.5 0.5 1]
                       :visible-tile-color   [1 1 1 1]
                       :invisible-tile-color [0 0 0 1]}))
  ctx)
