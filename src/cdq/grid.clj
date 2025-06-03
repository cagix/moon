(ns cdq.grid)

(defprotocol Grid
  (cell [_ int-position])
  (rectangle->cells [_ rectangle])
  (circle->cells [_ circle])
  (circle->entities [_ circle])
  (cells->entities [_ cells])
  (cached-adjacent-cells [_ cell])
  (point->entities [_ position])
  (add-entity! [_ eid])
  (remove-entity! [_ eid])
  (position-changed! [_ eid])
  (valid-position? [_ new-body]))

; using this instead of g2d/get-8-neighbour-positions, because `for` there creates a lazy seq.
(let [offsets [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]]]
  (defn get-8-neighbour-positions [position] ; -> utils
    (mapv #(mapv + position %) offsets)))

#_(defn- get-8-neighbour-positions [[x y]]
    (mapv (fn [tx ty]
            [tx ty])
          (range (dec x) (+ x 2))
          (range (dec y) (+ y 2))))
