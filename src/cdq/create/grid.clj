(ns cdq.create.grid
  (:require [cdq.context :as context]
            [cdq.grid :as grid]
            [gdl.data.grid2d :as g2d]
            [gdl.gdx.tiled :as tiled]
            [gdl.utils :refer [tile->middle defcomponent]]))

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

(defn ->grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn create [tiled-map]
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

(defn- add-entity [grid eid]
  (set-cells! grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! grid eid)))

(defn- remove-entity [eid]
  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn- entity-position-changed [grid eid]
  (remove-from-cells! eid)
  (set-cells! grid eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! grid eid)))

(defcomponent :cdq.context/grid
  (context/add-entity [[_ grid] eid]
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (add-entity grid eid))

  (context/remove-entity [[_ grid] eid]
    (remove-entity eid))

  (context/position-changed [[_ grid] eid]
    (entity-position-changed grid eid)))
