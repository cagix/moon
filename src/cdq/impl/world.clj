(ns cdq.impl.world
  (:require [cdq.ctx :as ctx]
            [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.utils :as utils]
            [cdq.vector2 :as v]
            [gdl.tiled :as tiled]))

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
    (-> this faction :distance)))

(defn- ->grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (utils/tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn create-grid [tiled-map]
  (g2d/create-grid
   (tiled/tm-width tiled-map)
   (tiled/tm-height tiled-map)
   (fn [position]
     (atom (->grid-cell position
                        (case (tiled/movement-property tiled-map position)
                          "none" :none
                          "air"  :air
                          "all"  :all))))))

(defn- set-cells! [grid eid]
  (let [cells (grid/rectangle->cells grid @eid)]
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
    (grid/rectangle->cells grid rectangle)
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

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn create-raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell cell/blocks-vision?))
    [arr width height]))

(defn- add-entity! [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! ctx/entity-ids assoc id eid))
  (content-grid/add-entity! ctx/content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (set-cells! ctx/grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! ctx/grid eid)))

(defn remove-entity! [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? @ctx/entity-ids id))
    (swap! ctx/entity-ids dissoc id))
  (content-grid/remove-entity! eid)
  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn position-changed! [eid]
  (content-grid/position-changed! ctx/content-grid eid)
  (remove-from-cells! eid)
  (set-cells! ctx/grid eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! ctx/grid eid)))

(defrecord Body [position
                 left-bottom

                 width
                 height
                 half-width
                 half-height
                 radius

                 collides?
                 z-order
                 rotation-angle]
  entity/Entity
  (in-range? [entity target* maxrange] ; == circle-collides?
    (< (- (float (v/distance (:position entity)
                             (:position target*)))
          (float (:radius entity))
          (float (:radius target*)))
       (float maxrange))))

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? ctx/minimum-size 0)))
  (assert (>= height (if collides? ctx/minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set ctx/z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v])))
          {}
          components))

(defn spawn-entity! [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (utils/safe-merge (-> components
                                            (assoc :entity/id (swap! ctx/id-counter inc))
                                            create-vs))))]
    (add-entity! eid)
    (doseq [component @eid]
      (utils/handle-txs! (entity/create! component eid)))))
