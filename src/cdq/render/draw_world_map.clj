(ns cdq.render.draw-world-map
  (:require [cdq.raycaster :as raycaster]
            [cdq.tile-color-setter :as tile-color-setter]
            [clojure.gdx :as gdx]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as camera]))

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
                              :explored-tile-color  (gdx/color [0.5 0.5 0.5 1])
                              :visible-tile-color   (gdx/color :white)
                              :invisible-tile-color (gdx/color :black)}))
  ctx)
