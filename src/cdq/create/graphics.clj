(ns cdq.create.graphics
  (:require [cdq.g :as g]
            [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.application]
            [gdl.graphics :as graphics]
            [gdl.tiled :as tiled]
            [gdl.utils :as utils]))

(defn add [ctx config]
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))]
    (merge ctx
           {:ctx/batch batch
            :ctx/unit-scale 1
            :ctx/world-unit-scale world-unit-scale
            :ctx/shape-drawer-texture shape-drawer-texture
            :ctx/shape-drawer (sd/create (:java-object batch)
                                         (graphics/texture-region shape-drawer-texture 1 0 1 1))
            :ctx/cursors (utils/mapvals
                          (fn [[file [hotspot-x hotspot-y]]]
                            (graphics/create-cursor (format (:cursor-path-format config) file)
                                                    hotspot-x
                                                    hotspot-y))
                          (:cursors config))
            :ctx/default-font (graphics/truetype-font (:default-font config))
            :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                               (tiled/renderer tiled-map
                                                               world-unit-scale
                                                               (:java-object batch))))})))

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
