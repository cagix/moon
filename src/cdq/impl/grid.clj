(ns cdq.impl.grid
  (:require [cdq.entity.faction :as faction]
            [cdq.position :as position]
            [gdl.grid2d :as g2d]
            [cdq.world.grid.cell :as cell]
            [cdq.gdx.math.geom :as geom]
            [cdq.world.grid :as grid]))

(defn- body->occupied-cells
  [grid
   {:keys [body/position
           body/width
           body/height]
    :as body}]
  (if (or (> (float width) 1) (> (float height) 1))
    (g2d/get-cells grid (geom/body->touched-tiles body))
    [(grid (mapv int position))]))

(extend-type gdl.grid2d.VectorGrid
  cdq.world.grid/Grid
  (circle->cells [g2d circle]
    (->> circle
         geom/circle->outer-rectangle
         geom/rectangle->touched-tiles
         (g2d/get-cells g2d)))

  (cells->entities [_ cells]
    (into #{} (mapcat :entities) cells))

  (circle->entities [g2d {:keys [position radius] :as circle}]
    (->> (grid/circle->cells g2d circle)
         (map deref)
         (grid/cells->entities g2d)
         (filter #(geom/overlaps?
                   (geom/circle (position 0) (position 1) radius)
                   (geom/body->gdx-rectangle (:entity/body @%))))))

  (cached-adjacent-cells [g2d cell]
    (if-let [result (:adjacent-cells @cell)]
      result
      (let [result (->> @cell
                        :position
                        position/get-8-neighbours
                        (g2d/get-cells g2d))]
        (swap! cell assoc :adjacent-cells result)
        result)))

  (point->entities [g2d position]
    (when-let [cell (g2d (mapv int position))]
      (filter #(geom/contains? (geom/body->gdx-rectangle (:entity/body @%)) position)
              (:entities @cell))))

  (set-touched-cells! [grid eid]
    (let [cells (g2d/get-cells grid (geom/body->touched-tiles (:entity/body @eid)))]
      (assert (not-any? nil? cells))
      (swap! eid assoc ::touched-cells cells)
      (doseq [cell cells]
        (assert (not (get (:entities @cell) eid)))
        (swap! cell update :entities conj eid))))

  (remove-from-touched-cells! [_ eid]
    (doseq [cell (::touched-cells @eid)]
      (assert (get (:entities @cell) eid))
      (swap! cell update :entities disj eid)))

  (set-occupied-cells! [grid eid]
    (let [cells (body->occupied-cells grid (:entity/body @eid))]
      (doseq [cell cells]
        (assert (not (get (:occupied @cell) eid)))
        (swap! cell update :occupied conj eid))
      (swap! eid assoc ::occupied-cells cells)))

  (remove-from-occupied-cells! [_ eid]
    (doseq [cell (::occupied-cells @eid)]
      (assert (get (:occupied @cell) eid))
      (swap! cell update :occupied disj eid)))

  (valid-position? [g2d {:keys [body/z-order] :as body} entity-id]
    {:pre [(:body/collides? body)]}
    (let [cells* (into [] (map deref) (g2d/get-cells g2d (geom/body->touched-tiles body)))]
      (and (not-any? #(cell/blocked? % z-order) cells*)
           (->> cells*
                (grid/cells->entities g2d)
                (not-any? (fn [other-entity]
                            (let [other-entity @other-entity]
                              (and (not= (:entity/id other-entity) entity-id)
                                   (:body/collides? (:entity/body other-entity))
                                   (geom/overlaps? (geom/body->gdx-rectangle (:entity/body other-entity))
                                                   (geom/body->gdx-rectangle body))))))))))

  (nearest-enemy-distance [grid entity]
    (cell/nearest-entity-distance @(grid (mapv int (:body/position (:entity/body entity))))
                                  (faction/enemy (:entity/faction entity))))

  (nearest-enemy [grid entity]
    (cell/nearest-entity @(grid (mapv int (:body/position (:entity/body entity))))
                         (faction/enemy (:entity/faction entity)))))

(defrecord RCell [position
                  middle
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  cell/Cell
  (blocked? [_ z-order]
    (case movement
      :none true
      :air (case z-order
             :z-order/flying false
             :z-order/ground true)
      :all false))

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
  (map->RCell
   {:position position
    :middle (mapv (partial + 0.5) position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn create [width height cell-movement]
  (g2d/create-grid width
                   height
                   (fn [position]
                     (atom (create-grid-cell position (cell-movement position))))))
