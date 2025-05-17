(ns cdq.cell
  (:require [cdq.utils :as utils]))

(defprotocol Cell
  (blocked? [_ z-order])
  (blocks-vision? [_])
  (occupied-by-other? [_ eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [_ faction])
  (nearest-entity-distance [_ faction]))

(defn pf-blocked? [cell]
  (blocked? cell :z-order/ground))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  Cell
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

(defn create [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (atom (map->RCell
         {:position position
          :middle (utils/tile->middle position)
          :movement movement
          :entities #{}
          :occupied #{}})))
