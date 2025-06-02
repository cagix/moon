(ns clojure.render.draw-world-map
  (:require [clojure.ctx :as ctx]
            [clojure.graphics.tiled-map-renderer :as tiled-map-renderer]))

(defn do! [{:keys [ctx/tiled-map
                   ctx/tiled-map-renderer
                   ctx/world-viewport]
            :as ctx}]
  (tiled-map-renderer/draw! (tiled-map-renderer tiled-map)
                            tiled-map
                            (ctx/tile-color-setter ctx)
                            (:camera world-viewport))
  ctx)
