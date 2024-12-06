(ns forge.world.grid
  (:require [clojure.gdx.math.shapes :refer [rectangle->tiles
                                             circle->outer-rectangle
                                             rect-contains?
                                             overlaps?]]
            [clojure.gdx.tiled :as tiled]
            [data.grid2d :as g2d]
            [forge.utils :refer [bind-root tile->middle ->tile]]))

(defprotocol Cell
  (cell-blocked? [cell* z-order])
  (blocks-vision? [cell*])
  (occupied-by-other? [cell* eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [cell* faction])
  (nearest-entity-distance [cell* faction]))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  Cell
  (cell-blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied)) ; contains? faster?

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(declare world-grid)

(defn init [tiled-map]
  (bind-root world-grid (g2d/create-grid
                         (tiled/tm-width tiled-map)
                         (tiled/tm-height tiled-map)
                         (fn [position]
                           (atom (->cell position
                                         (case (tiled/movement-property tiled-map position)
                                           "none" :none
                                           "air"  :air
                                           "all"  :all)))))))

(defn rectangle->cells [rectangle]
  (into [] (keep world-grid) (rectangle->tiles rectangle)))

(defn circle->cells [circle]
  (->> circle
       circle->outer-rectangle
       rectangle->cells))

(defn cells->entities [cells]
  (into #{} (mapcat :entities) cells))

(defn circle->entities [circle]
  (->> (circle->cells circle)
       (map deref)
       cells->entities
       (filter #(overlaps? circle @%))))

(defn- set-cells! [eid]
  (let [cells (rectangle->cells @eid)]
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
(defn- rectangle->occupied-cells [{:keys [left-bottom width height] :as rectangle}]
  (if (or (> (float width) 1) (> (float height) 1))
    (rectangle->cells rectangle)
    [(get world-grid
          [(int (+ (float (left-bottom 0)) (/ (float width) 2)))
           (int (+ (float (left-bottom 1)) (/ (float height) 2)))])]))

(defn- set-occupied-cells! [eid]
  (let [cells (rectangle->occupied-cells @eid)]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn- remove-from-occupied-cells! [eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(def ^:private offsets [[-1 -1] [-1 0] [-1 1] [0 -1] [0 1] [1 -1] [1 0] [1 1]])

; using this instead of g2d/get-8-neighbour-positions, because `for` there creates a lazy seq.
(defn get-8-neighbour-positions [position]
  (mapv #(mapv + position %) offsets))

#_(defn- get-8-neighbour-positions [[x y]]
    (mapv (fn [tx ty]
            [tx ty])
          (range (dec x) (+ x 2))
          (range (dec y) (+ y 2))))

(defn cached-adjacent-cells [cell]
  (if-let [result (:adjacent-cells @cell)]
    result
    (let [result (into [] (keep world-grid) (-> @cell :position get-8-neighbour-positions))]
      (swap! cell assoc :adjacent-cells result)
      result)))

(defn point->entities [position]
  (when-let [cell (get world-grid (->tile position))]
    (filter #(rect-contains? @% position)
            (:entities @cell))))

(defn add-entity [eid]
  (set-cells! eid)
  (when (:collides? @eid)
    (set-occupied-cells! eid)))

(defn remove-entity [eid]
  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn entity-position-changed [eid]
  (remove-from-cells! eid)
  (set-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! eid)))
