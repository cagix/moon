(ns cdq.world.grid
  (:require [cdq.world.grid.cell :as cell]
            [cdq.world.entity :as entity]))

(defprotocol Grid
  (cell [_ int-position])
  (cells [_ int-positions])
  (body->cells [_ body])
  (circle->cells [_ circle])
  (circle->entities [_ circle])
  (cells->entities [_ cells])
  (cached-adjacent-cells [_ cell])
  (point->entities [_ position])
  (add-entity! [_ eid])
  (remove-entity! [_ eid])
  (position-changed! [_ eid])
  (valid-position? [_ new-body entity-id]))

; using this instead of g2d/get-8-neighbour-positions, because `for` there creates a lazy seq.
(let [offsets [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]]]
  (defn get-8-neighbour-positions [position] ; -> utils
    (mapv #(mapv + position %) offsets)))

#_(defn- get-8-neighbour-positions [[x y]]
    (mapv (fn [tx ty]
            [tx ty])
          (range (dec x) (+ x 2))
          (range (dec y) (+ y 2))))

(defn nearest-enemy-distance [grid entity]
  (cell/nearest-entity-distance @(cell grid (mapv int (entity/position entity)))
                                (entity/enemy entity)))

(defn nearest-enemy [grid entity]
  (cell/nearest-entity @(cell grid (mapv int (entity/position entity)))
                       (entity/enemy entity)))
