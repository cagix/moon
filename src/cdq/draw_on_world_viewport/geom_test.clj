(ns cdq.draw-on-world-viewport.geom-test
  (:require [cdq.gdx.math.geom :as geom]
            [cdq.world.grid :as grid]))

(defn do!
  [{:keys [ctx/grid
           ctx/world-mouse-position]}]
  (let [position world-mouse-position
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{:keys [x y width height]} (geom/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))
