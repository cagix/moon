(ns cdq.game.geom-test
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [cdq.math :as math]
            [cdq.world.grid :as grid]))

(defn do! []
  (let [g ctx/graphics
        position (graphics/world-mouse-position g)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/draw-circle g position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells (:grid ctx/world) circle))]
      (graphics/draw-rectangle g x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (math/circle->outer-rectangle circle)]
      (graphics/draw-rectangle g x y width height [0 0 1 1]))))
