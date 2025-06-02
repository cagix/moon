(ns clojure.render.draw-on-world-viewport.geom-test
  (:require [clojure.grid :as grid]
            [clojure.ctx :as ctx]
            [clojure.math.geom :as geom]))

(defn- geom-test* [{:keys [ctx/grid] :as ctx}]
  (let [position (ctx/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{[x y] :left-bottom
                 :keys [width height]} (geom/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn do! [ctx]
  (ctx/handle-draws! ctx (geom-test* ctx)))
