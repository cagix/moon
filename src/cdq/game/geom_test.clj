(ns cdq.game.geom-test
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.grid :as grid]
            [cdq.math :as math]
            [cdq.viewport :as viewport]))

(defn do! []
  (let [position (viewport/mouse-position ctx/world-viewport)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/draw-circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells (:grid ctx/world) circle))]
      (graphics/draw-rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (math/circle->outer-rectangle circle)]
      (graphics/draw-rectangle x y width height [0 0 1 1]))))
