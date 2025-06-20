(ns cdq.grid-impl
  (:require [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.math.geom :as geom]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]))

(defn- body->touched-tiles
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

(defn- set-touched-cells! [grid eid]
  (let [cells (grid/rectangle->cells grid @eid)]
    (assert (not-any? nil? cells))
    (swap! eid assoc ::touched-cells cells)
    (doseq [cell cells]
      (assert (not (get (:entities @cell) eid)))
      (swap! cell update :entities conj eid))))

(defn- remove-from-touched-cells! [eid]
  (doseq [cell (::touched-cells @eid)]
    (assert (get (:entities @cell) eid))
    (swap! cell update :entities disj eid)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- entity->occupied-cells [grid {:keys [entity/position width height] :as rectangle}]
  (if (or (> (float width) 1) (> (float height) 1))
    (grid/rectangle->cells grid rectangle)
    [(grid/cell grid (mapv int position))]))

(defn- set-occupied-cells! [grid eid]
  (let [cells (entity->occupied-cells grid @eid)]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn- remove-from-occupied-cells! [eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(deftype Grid [g2d]
  grid/Grid
  (cell [_ position]
    (g2d position))

  (cells [_ int-positions]
    (into [] (keep g2d) int-positions))

  (rectangle->cells [this rectangle]
    (grid/cells this (body->touched-tiles rectangle)))

  (circle->cells [this circle]
    (->> circle
         geom/circle->outer-rectangle
         (grid/rectangle->cells this)))

  (circle->entities [this {:keys [position radius] :as circle}]
    (->> (grid/circle->cells this circle)
         (map deref)
         (grid/cells->entities this)
         (filter #(geom/overlaps?
                   (geom/circle (position 0) (position 1) radius)
                   (entity/rectangle @%)))))

  (cells->entities [_ cells]
    (into #{} (mapcat :entities) cells))

  (cached-adjacent-cells [this cell]
    (if-let [result (:adjacent-cells @cell)]
      result
      (let [result (->> @cell
                        :position
                        grid/get-8-neighbour-positions
                        (grid/cells this))]
        (swap! cell assoc :adjacent-cells result)
        result)))

  (point->entities [this position]
    (when-let [cell (grid/cell this (mapv int position))]
      (filter #(geom/contains? (entity/rectangle @%) position)
              (:entities @cell))))

  (add-entity! [this eid]
    (set-touched-cells! this eid)
    (when (:collides? @eid)
      (set-occupied-cells! this eid)))

  (remove-entity! [_ eid]
    (remove-from-touched-cells! eid)
    (when (:collides? @eid)
      (remove-from-occupied-cells! eid)))

  (position-changed! [this eid]
    (remove-from-touched-cells! eid)
    (set-touched-cells! this eid)
    (when (:collides? @eid)
      (remove-from-occupied-cells! eid)
      (set-occupied-cells! this eid)))

  (valid-position? [this {:keys [z-order] :as body}]
    {:pre [(:collides? body)]}
    (let [cells* (into [] (map deref) (grid/rectangle->cells this body))]
      (and (not-any? #(cell/blocked? % z-order) cells*)
           (->> cells*
                (grid/cells->entities this)
                (not-any? (fn [other-entity]
                            (let [other-entity @other-entity]
                              (and (not= (entity/id other-entity) (entity/id body))
                                   (:collides? other-entity)
                                   (entity/overlaps? other-entity body))))))))))

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
  (->Grid
   (g2d/create-grid (:tiled-map/width  tiled-map)
                    (:tiled-map/height tiled-map)
                    (fn [position]
                      (create-grid-cell position
                                        ; also do at level creation, here no tiled foozaboozls.
                                        (case (tiled/movement-property tiled-map position)
                                          "none" :none
                                          "air"  :air
                                          "all"  :all))))))
