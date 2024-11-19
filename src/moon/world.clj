(ns moon.world
  (:require [gdl.utils :refer [tile->middle]]
            [moon.world.grid :as grid]))

(declare grid
         tick-error
         paused?)

(defn cell [position]
  (get grid position))

(defn rectangle->cells        [rectangle] (grid/rectangle->cells        grid rectangle))
(defn circle->cells           [circle]    (grid/circle->cells           grid circle))
(defn circle->entities        [circle]    (grid/circle->entities        grid circle))
(defn cached-adjacent-cells   [cell]      (grid/cached-adjacent-cells   grid cell))
(defn point->entities         [position]  (grid/point->entities         grid position))
(defn add-entity              [eid]       (grid/add-entity              grid eid))
(defn remove-entity           [eid]       (grid/remove-entity           grid eid))
(defn entity-position-changed [eid]       (grid/entity-position-changed grid eid))
(def cells->entities grid/cells->entities)

(defprotocol GridCell
  (blocked? [cell* z-order])
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
  GridCell
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
    (some #(not= % eid) occupied)) ; contains? faster?

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn ->cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))
