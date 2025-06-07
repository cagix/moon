(ns cdq.render.draw-world-map
  (:require [cdq.raycaster :as raycaster]
            [cdq.tile-color-setter :as tile-color-setter]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]))

(defn do! [{:keys [ctx/graphics
                   ctx/tiled-map
                   ctx/explored-tile-corners
                   ctx/raycaster]
            :as ctx}]
  (graphics/draw-tiled-map! graphics
                            tiled-map
                            (tile-color-setter/create
                             {:ray-blocked? (fn [start end] (raycaster/blocked? raycaster start end))
                              :explored-tile-corners explored-tile-corners
                              :light-position (camera/position (:camera (:world-viewport graphics)))
                              :see-all-tiles? false
                              :explored-tile-color  (color/create [0.5 0.5 0.5 1])
                              :visible-tile-color   (color/create :white)
                              :invisible-tile-color (color/create :black)}))
  ctx)
