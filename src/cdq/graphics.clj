(ns cdq.graphics
  (:require [clojure.space.earlygrey.shape-drawer :as sd]
            [gdl.application]
            [gdl.c :as c]
            [gdl.graphics :as graphics]))

(extend-type gdl.application.Context
  c/Graphics
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
    (graphics/sprite (graphics/texture-region (c/texture ctx texture-path))
                     world-unit-scale))

  (sub-sprite [{:keys [ctx/world-unit-scale]} sprite [x y w h]]
    (graphics/sprite (graphics/sub-region (:texture-region sprite) x y w h)
                     world-unit-scale))

  (sprite-sheet [{:keys [ctx/world-unit-scale] :as ctx} texture-path tilew tileh]
    {:image (graphics/sprite (graphics/texture-region (c/texture ctx texture-path))
                             world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [ctx {:keys [image tilew tileh]} [x y]]
    (c/sub-sprite ctx image [(* x tilew) (* y tileh) tilew tileh])))
