(ns cdq.render.draw-on-world-viewport.geom-test
  (:require [cdq.grid :as grid]
            [cdq.math.geom :as geom]
            [gdl.c :as c]
            [gdl.graphics :as graphics]))

(defn- geom-test* [{:keys [ctx/world] :as ctx}]
  (let [grid (:world/grid world)
        position (c/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{:keys [x y width height]} (geom/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn do! [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (geom-test* ctx)))
