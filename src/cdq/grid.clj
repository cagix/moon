(ns cdq.grid
  (:require [gdl.math.shapes :refer [rectangle->tiles
                                     circle->outer-rectangle
                                     overlaps?
                                     rect-contains?]]))

(defn rectangle->cells [grid rectangle]
  (into [] (keep grid) (rectangle->tiles rectangle)))

(defn circle->cells [grid circle]
  (->> circle
       circle->outer-rectangle
       (rectangle->cells grid)))

(defn cells->entities [cells]
  (into #{} (mapcat :entities) cells))

(defn circle->entities [grid circle]
  (->> (circle->cells grid circle)
       (map deref)
       cells->entities
       (filter #(overlaps? circle @%))))

; using this instead of g2d/get-8-neighbour-positions, because `for` there creates a lazy seq.
(let [offsets [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]]]
  (defn get-8-neighbour-positions [position]
    (mapv #(mapv + position %) offsets)))

#_(defn- get-8-neighbour-positions [[x y]]
    (mapv (fn [tx ty]
            [tx ty])
          (range (dec x) (+ x 2))
          (range (dec y) (+ y 2))))

(defn cached-adjacent-cells [grid cell]
  (if-let [result (:adjacent-cells @cell)]
    result
    (let [result (into [] (keep grid) (-> @cell :position get-8-neighbour-positions))]
      (swap! cell assoc :adjacent-cells result)
      result)))

(defn point->entities [grid position]
  (when-let [cell (grid (mapv int position))]
    (filter #(rect-contains? @% position)
            (:entities @cell))))

(defprotocol Cell
  (blocked? [cell z-order])
  (blocks-vision? [cell])
  (occupied-by-other? [cell eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [cell faction])
  (nearest-entity-distance [cell faction]))
