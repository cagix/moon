(ns cdq.render.draw-on-world-viewport.geom-test
  (:require [clojure.ctx]
            [cdq.grid :as grid]
            [cdq.math :as math]
            [gdl.ctx :as ctx]))

(defn- geom-test* [{:keys [ctx/grid] :as ctx}]
  (let [position (ctx/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{[x y] :left-bottom
                 :keys [width height]} (math/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn do! [ctx]
  (clojure.ctx/handle-draws! ctx (geom-test* ctx)))
