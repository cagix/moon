(ns ^:no-doc moon.world.grid
  (:require #_[data.grid2d :as g2d]
            [forge.math.shape :as shape]
            [forge.utils :refer [->tile]]))

(defn- rectangle->tiles
  [{[x y] :left-bottom :keys [left-bottom width height]}]
  {:pre [left-bottom width height]}
  (let [x       (float x)
        y       (float y)
        width   (float width)
        height  (float height)
        l (int x)
        b (int y)
        r (int (+ x width))
        t (int (+ y height))]
    (set
     (if (or (> width 1) (> height 1))
       (for [x (range l (inc r))
             y (range b (inc t))]
         [x y])
       [[l b] [l t] [r b] [r t]]))))

(defn rectangle->cells [grid rectangle]
  (into [] (keep grid) (rectangle->tiles rectangle)))

(defn circle->cells [grid circle]
  (->> circle
       shape/circle->outer-rectangle
       (rectangle->cells grid)))

(defn cells->entities [cells]
  (into #{} (mapcat :entities) cells))

(defn circle->entities [grid circle]
  (->> (circle->cells grid circle)
       (map deref)
       cells->entities
       (filter #(shape/overlaps? circle @%))))

(defn- set-cells! [grid eid]
  (let [cells (rectangle->cells grid @eid)]
    (assert (not-any? nil? cells))
    (swap! eid assoc ::touched-cells cells)
    (doseq [cell cells]
      (assert (not (get (:entities @cell) eid)))
      (swap! cell update :entities conj eid))))

(defn- remove-from-cells! [eid]
  (doseq [cell (::touched-cells @eid)]
    (assert (get (:entities @cell) eid))
    (swap! cell update :entities disj eid)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- rectangle->occupied-cells [grid {:keys [left-bottom width height] :as rectangle}]
  (if (or (> (float width) 1) (> (float height) 1))
    (rectangle->cells grid rectangle)
    [(get grid
          [(int (+ (float (left-bottom 0)) (/ (float width) 2)))
           (int (+ (float (left-bottom 1)) (/ (float height) 2)))])]))

(defn- set-occupied-cells! [grid eid]
  (let [cells (rectangle->occupied-cells grid @eid)]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn- remove-from-occupied-cells! [eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(defn- get-8-neighbour-positions [[x y]]
  (mapv (fn [tx ty]
          [tx ty])
   (range (dec x) (+ x 2))
   (range (dec y) (+ y 2))))

(def ^:private offsets [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]])

; using this instead of g2d/get-8-neighbour-positions, because `for` there creates a lazy seq.
(defn- get-8-neighbour-positions [position]
  (mapv #(mapv + position %) offsets))

(defn cached-adjacent-cells [grid cell]
  (if-let [result (:adjacent-cells @cell)]
    result
    (let [result (into [] (keep grid) (-> @cell :position get-8-neighbour-positions))]
      (swap! cell assoc :adjacent-cells result)
      result)))

(defn point->entities [grid position]
  (when-let [cell (get grid (->tile position))]
    (filter #(shape/contains? @% position)
            (:entities @cell))))

(defn add-entity [grid eid]
  (set-cells! grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! grid eid)))

(defn remove-entity [eid]
  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn entity-position-changed [grid eid]
  (remove-from-cells! eid)
  (set-cells! grid eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! grid eid)))
