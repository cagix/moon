(ns cdq.grid
  (:require [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.grid2d :as g2d]
            [cdq.math :as math]
            [cdq.utils :as utils]
            [gdl.math]
            [gdl.tiled :as tiled]))

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
       math/circle->outer-rectangle
       (rectangle->cells grid)))

(defn cells->entities [cells]
  (into #{} (mapcat :entities) cells))

(defn circle->entities [grid
                        {:keys [position radius] :as circle}]
  (->> (circle->cells grid circle)
       (map deref)
       cells->entities
       (filter #(gdl.math/overlaps?
                 (gdl.math/circle (position 0)
                                  (position 1)
                                  radius)
                 (entity/rectangle @%)))))

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
    (filter #(gdl.math/contains? (entity/rectangle @%)
                                 position)
            (:entities @cell))))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  cell/Cell
  (blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied))

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance))

  (pf-blocked? [this]
    (cell/blocked? this :z-order/ground)))

(defn- create-grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (atom (map->RCell
         {:position position
          :middle (utils/tile->middle position)
          :movement movement
          :entities #{}
          :occupied #{}})))

(defn create [tiled-map]
  (g2d/create-grid (tiled/tm-width  tiled-map)
                   (tiled/tm-height tiled-map)
                   (fn [position]
                     (create-grid-cell position
                                       (case (tiled/movement-property tiled-map position)
                                         "none" :none
                                         "air"  :air
                                         "all"  :all)))))

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
    [(grid [(int (+ (float (left-bottom 0)) (/ (float width) 2)))
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

(defn add-entity! [grid eid]
  (set-cells! grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! grid eid)))

(defn remove-entity! [eid]
  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn position-changed! [grid eid]
  (remove-from-cells! eid)
  (set-cells! grid eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! grid eid)))

(defn valid-position? [grid {:keys [z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (rectangle->cells grid body))]
    (and (not-any? #(cell/blocked? % z-order) cells*)
         (->> cells*
              cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (entity/id other-entity) (entity/id body))
                                 (:collides? other-entity)
                                 (entity/overlaps? other-entity body)))))))))
