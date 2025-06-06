(ns cdq.render.draw-world-map
  (:require [cdq.ctx :as ctx]
            [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics
                   ctx/tiled-map]
            :as ctx}]
  (graphics/draw-tiled-map! graphics tiled-map (ctx/tile-color-setter ctx))
  ctx)
