(ns cdq.render.draw-world-map
  (:require [cdq.tile-color-setter :as tile-color-setter]
            [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics
                   ctx/tiled-map]
            :as ctx}]
  (graphics/draw-tiled-map! graphics tiled-map (tile-color-setter/create ctx))
  ctx)
