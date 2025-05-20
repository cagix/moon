(ns cdq.application.render.draw-tiled-map
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.draw-tiled-map :as draw-tiled-map]))

(defn do! []
  (draw-tiled-map/do! {:ctx/get-tiled-map-renderer ctx/get-tiled-map-renderer
                       :ctx/tiled-map ctx/tiled-map
                       :ctx/raycaster ctx/raycaster
                       :ctx/explored-tile-corners ctx/explored-tile-corners
                       :ctx/world-viewport ctx/world-viewport}))
