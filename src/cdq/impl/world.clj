(ns cdq.impl.world
  (:require [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.tiled :as tiled]
            [cdq.utils :as utils]
            [cdq.world :as world]))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  grid/Cell
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

(defn- create-grid [tiled-map]
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

(defn- create-raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell grid/blocks-vision?))
    [arr width height]))

(defrecord World [tiled-map
                  grid
                  raycaster
                  content-grid
                  explored-tile-corners
                  entity-ids
                  potential-field-cache
                  active-entities]
  world/World
  (add-entity! [_ eid]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/update-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (set-cells! grid eid)
    (when (:collides? @eid)
      (set-occupied-cells! grid eid)))

  (remove-entity! [_ eid]
    (let [id (:entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (remove-from-cells! eid)
    (when (:collides? @eid)
      (remove-from-occupied-cells! eid)))

  (position-changed! [_ eid]
    (content-grid/update-entity! content-grid eid)
    (remove-from-cells! eid)
    (set-cells! grid eid)
    (when (:collides? @eid)
      (remove-from-occupied-cells! eid)
      (set-occupied-cells! grid eid)))

  (cell [_ position]
    ; assert/document integer ?
    (grid position)))

(defn create [{:keys [tiled-map start-position]}]
  (let [width  (tiled/tm-width  tiled-map)
        height (tiled/tm-height tiled-map)
        grid (create-grid tiled-map)]
    (map->World {:tiled-map tiled-map
                 :start-position start-position
                 :grid grid
                 :raycaster (create-raycaster grid)
                 :content-grid (content-grid/create {:cell-size 16
                                                     :width  width
                                                     :height height})
                 :explored-tile-corners (atom (g2d/create-grid width
                                                               height
                                                               (constantly false)))
                 :entity-ids (atom {})
                 :potential-field-cache (atom nil)
                 :active-entities nil})))
