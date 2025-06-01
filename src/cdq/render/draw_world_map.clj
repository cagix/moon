(ns cdq.render.draw-world-map
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as g]))

(defn do! [{:keys [ctx/tiled-map] :as ctx}]
  (g/draw-tiled-map! ctx
                     tiled-map
                     (ctx/tile-color-setter ctx))
  ctx)
