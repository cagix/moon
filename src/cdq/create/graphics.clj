(ns cdq.create.graphics
  (:require [cdq.g :as g]
            [cdq.graphics :as graphics]
            [gdl.assets :as assets]
            [gdl.application]))

(def ^:private -k :ctx/graphics)

(defn add [ctx config]
  (assoc ctx -k (graphics/create config)))

(extend-type gdl.application.Context
  g/Graphics
  (sprite [{:keys [ctx/assets] :as ctx} texture-path] ; <- textures should be inside graphics, makes this easier.
    (graphics/sprite (-k ctx)
                     (assets/texture assets texture-path)))

  (sub-sprite [ctx sprite [x y w h]]
    (graphics/sub-sprite (-k ctx)
                         sprite
                         [x y w h]))

  (sprite-sheet [{:keys [ctx/assets] :as ctx} texture-path tilew tileh]
    (graphics/sprite-sheet (-k ctx)
                           (assets/texture assets texture-path)
                           tilew
                           tileh))

  (sprite-sheet->sprite [ctx sprite-sheet [x y]]
    (graphics/sprite-sheet->sprite (-k ctx)
                                   sprite-sheet
                                   [x y])))

(extend-type gdl.application.Context
  g/WorldViewport
  (set-camera-position! [ctx position]
    (graphics/set-camera-position! (-k ctx) position))

  (world-mouse-position [ctx]
    (graphics/world-mouse-position (-k ctx)))

  (world-viewport-width [ctx]
    (graphics/world-viewport-width (-k ctx)))

  (world-viewport-height [ctx]
    (graphics/world-viewport-height (-k ctx)))

  (camera-position [ctx]
    (graphics/camera-position (-k ctx)))

  (inc-zoom! [ctx amount]
    (graphics/inc-zoom! (-k ctx) amount))

  (camera-frustum [ctx]
    (graphics/camera-frustum (-k ctx)))

  (visible-tiles [ctx]
    (graphics/visible-tiles (-k ctx)))

  (camera-zoom [ctx]
    (graphics/camera-zoom (-k ctx))))
