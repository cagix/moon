(ns cdq.render.draw-world-map
  (:require [cdq.ctx :as ctx]
            [gdl.graphics.tiled-map-renderer :as tiled-map-renderer]))

(defn do! [{:keys [ctx/graphics
                   ctx/tiled-map
                   ctx/world-viewport]
            :as ctx}]
  (tiled-map-renderer/draw! ((:tiled-map-renderer graphics) tiled-map)
                            tiled-map
                            (ctx/tile-color-setter ctx)
                            (:camera world-viewport))
  ctx)
