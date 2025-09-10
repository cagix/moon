(ns cdq.render.draw-world-map
  (:require [cdq.raycaster :as raycaster]
            [cdq.tile-color-setter :as tile-color-setter]
            [clojure.gdx.graphics.tiled-map-renderer :as tm-renderer]))

(defn do!
  [{:keys [ctx/explored-tile-corners
           ctx/tiled-map-renderer
           ctx/raycaster
           ctx/world
           ctx/world-viewport]}]
  (tm-renderer/draw! tiled-map-renderer
                     world-viewport
                     (:world/tiled-map world)
                     (tile-color-setter/create
                      {:ray-blocked? (partial raycaster/blocked? raycaster)
                       :explored-tile-corners explored-tile-corners
                       :light-position (:camera/position (:viewport/camera world-viewport))
                       :see-all-tiles? false
                       :explored-tile-color  [0.5 0.5 0.5 1]
                       :visible-tile-color   [1 1 1 1]
                       :invisible-tile-color [0 0 0 1]})))
