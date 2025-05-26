(ns cdq.create.tiled-map-renderer
  (:require [gdl.application]
            [cdq.g :as g]
            [gdl.tiled :as tiled]))

(defn add [{:keys [ctx/batch
                   ctx/world-unit-scale]
            :as ctx}]
  (assoc ctx :ctx/tiled-map-renderer
         (memoize (fn [tiled-map]
                    (tiled/renderer tiled-map
                                    world-unit-scale
                                    (:java-object batch))))))

(extend-type gdl.application.Context
  g/TiledMapRenderer
  (draw-tiled-map! [{:keys [ctx/world-viewport
                            ctx/tiled-map-renderer]}
                    tiled-map
                    color-setter]
    (tiled/draw! (tiled-map-renderer tiled-map)
                 tiled-map
                 color-setter
                 (:camera world-viewport))))
