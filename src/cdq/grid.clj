(ns cdq.grid
  (:require [cdq.entity :as entity]
            [cdq.faction :as faction]
            [cdq.position :as position]
            [cdq.grid.cell :as cell]
            [cdq.gdx.math.geom :as geom]))

(defn cells [g2d int-positions]
  (into [] (keep g2d) int-positions))

(defn- body->occupied-cells [grid {:keys [body/position body/width body/height] :as body}]
  (if (or (> (float width) 1) (> (float height) 1))
    (cells grid (geom/body->touched-tiles body))
    [(grid (mapv int position))]))

(defn circle->cells [g2d circle]
  (->> circle
       geom/circle->outer-rectangle
       geom/rectangle->touched-tiles
       (cells g2d)))

(defn cells->entities [_ cells]
  (into #{} (mapcat :entities) cells))

(defn circle->entities [g2d {:keys [position radius] :as circle}]
  (->> (circle->cells g2d circle)
       (map deref)
       (cells->entities g2d)
       (filter #(geom/overlaps?
                 (geom/circle (position 0) (position 1) radius)
                 (geom/body->gdx-rectangle (:entity/body @%))))))

(defn cached-adjacent-cells [g2d cell]
  (if-let [result (:adjacent-cells @cell)]
    result
    (let [result (->> @cell
                      :position
                      position/get-8-neighbours
                      (cells g2d))]
      (swap! cell assoc :adjacent-cells result)
      result)))

(defn point->entities [g2d position]
  (when-let [cell (g2d (mapv int position))]
    (filter #(geom/contains? (geom/body->gdx-rectangle (:entity/body @%)) position)
            (:entities @cell))))

(defn set-touched-cells! [grid eid]
  (let [cells (cells grid (geom/body->touched-tiles (:entity/body @eid)))]
    (assert (not-any? nil? cells))
    (swap! eid assoc ::touched-cells cells)
    (doseq [cell cells]
      (assert (not (get (:entities @cell) eid)))
      (swap! cell update :entities conj eid))))

(defn remove-from-touched-cells! [_ eid]
  (doseq [cell (::touched-cells @eid)]
    (assert (get (:entities @cell) eid))
    (swap! cell update :entities disj eid)))

(defn set-occupied-cells! [grid eid]
  (let [cells (body->occupied-cells grid (:entity/body @eid))]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn remove-from-occupied-cells! [_ eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(defn valid-position? [g2d {:keys [body/z-order] :as body} entity-id]
  {:pre [(:body/collides? body)]}
  (let [cells* (into [] (map deref) (cells g2d (geom/body->touched-tiles body)))]
    (and (not-any? #(cell/blocked? % z-order) cells*)
         (->> cells*
              (cells->entities g2d)
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) entity-id)
                                 (:body/collides? (:entity/body other-entity))
                                 (geom/overlaps? (geom/body->gdx-rectangle (:entity/body other-entity))
                                                 (geom/body->gdx-rectangle body))))))))))

(defn nearest-enemy-distance [grid entity]
  (cell/nearest-entity-distance @(grid (mapv int (entity/position entity)))
                                (faction/enemy (:entity/faction entity))))

(defn nearest-enemy [grid entity]
  (cell/nearest-entity @(grid (mapv int (entity/position entity)))
                       (faction/enemy (:entity/faction entity))))
