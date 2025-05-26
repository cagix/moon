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

(extend-type gdl.application.Context
  g/Graphics
  (draw-on-world-viewport! [{:keys [ctx/batch
                                    ctx/world-viewport
                                    ctx/shape-drawer
                                    ctx/world-unit-scale]
                             :as ctx}
                            fns]
    (graphics/draw-on-viewport! batch
                                world-viewport
                                (fn []
                                  (sd/with-line-width shape-drawer world-unit-scale
                                    (fn []
                                      (doseq [f fns]
                                        (f (assoc ctx :ctx/unit-scale world-unit-scale))))))))

  (pixels->world-units [{:keys [ctx/world-unit-scale]} pixels]
    (* pixels world-unit-scale))

  (sprite [{:keys [ctx/world-unit-scale] :as ctx} texture-path]
    (graphics/sprite (graphics/texture-region (g/texture ctx texture-path))
                     world-unit-scale))

  (sub-sprite [{:keys [ctx/world-unit-scale]} sprite [x y w h]]
    (graphics/sprite (graphics/sub-region (:texture-region sprite) x y w h)
                     world-unit-scale))

  (sprite-sheet [{:keys [ctx/world-unit-scale] :as ctx} texture-path tilew tileh]
    {:image (graphics/sprite (graphics/texture-region (g/texture ctx texture-path))
                             world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [ctx {:keys [image tilew tileh]} [x y]]
    (g/sub-sprite ctx image [(* x tilew) (* y tileh) tilew tileh])))
