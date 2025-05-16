(ns cdq.game.draw-tiled-map
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))
(defn do! []
  (world/draw-tiled-map! ctx/world))
