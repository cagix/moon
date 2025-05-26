(ns gdl.application
  (:require [clojure.gdx.interop :as interop]
            [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.c :as c]
            [gdl.graphics :as graphics]
            [gdl.utils :as utils]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx Gdx)))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

(q/defrecord Context [ctx/assets
                      ctx/batch
                      ctx/unit-scale
                      ctx/world-unit-scale
                      ctx/shape-drawer-texture
                      ctx/shape-drawer
                      ctx/cursors
                      ctx/default-font
                      ctx/world-viewport
                      ctx/ui-viewport
                      ctx/tiled-map-renderer
                      ctx/stage]
  c/Graphics
  (draw-on-world-viewport! [ctx fns]
    (graphics/draw-on-viewport! batch
                                world-viewport
                                (fn []
                                  (sd/with-line-width shape-drawer world-unit-scale
                                    (fn []
                                      (doseq [f fns]
                                        (f (assoc ctx :ctx/unit-scale world-unit-scale))))))))

  (pixels->world-units [_ pixels]
    (* pixels world-unit-scale))

  (sprite [this texture-path]
    (graphics/sprite (graphics/texture-region (c/texture this texture-path))
                     world-unit-scale))

  (sub-sprite [_ sprite [x y w h]]
    (graphics/sprite (graphics/sub-region (:texture-region sprite) x y w h)
                     world-unit-scale))

  (sprite-sheet [this texture-path tilew tileh]
    {:image (graphics/sprite (graphics/texture-region (c/texture this texture-path))
                             world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [this {:keys [image tilew tileh]} [x y]]
    (c/sub-sprite this image [(* x tilew) (* y tileh) tilew tileh])))

(defn create-state! [config]
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))]
    (map->Context {:batch batch
                   :unit-scale 1
                   :world-unit-scale world-unit-scale
                   :shape-drawer-texture shape-drawer-texture
                   :shape-drawer (sd/create (:java-object batch)
                                            (graphics/texture-region shape-drawer-texture 1 0 1 1))
                   :cursors (utils/mapvals
                             (fn [[file [hotspot-x hotspot-y]]]
                               (graphics/create-cursor (format (:cursor-path-format config) file)
                                                       hotspot-x
                                                       hotspot-y))
                             (:cursors config))
                   :default-font (graphics/truetype-font (:default-font config))})))
