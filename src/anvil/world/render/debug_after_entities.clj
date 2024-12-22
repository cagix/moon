(ns anvil.world.render.debug-after-entities
  (:require [anvil.world :as world]
            [anvil.world.render :as render]
            [gdl.graphics :as g]
            [gdl.math.shapes :refer [circle->outer-rectangle]]))

(defn- geom-test []
  (let [position (g/world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (g/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (world/circle->cells circle))]
      (g/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (g/rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (g/world-mouse-position))
          cell (world/grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (g/rectangle x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5]))))))

(defn-impl render/debug-after-entities []
  #_(geom-test)
  (highlight-mouseover-tile))
