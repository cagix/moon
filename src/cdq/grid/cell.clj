(ns cdq.grid.cell
  (:require [cdq.utils :as utils]))

(defprotocol Cell
  (blocked? [_ z-order])
  (blocks-vision? [_])
  (occupied-by-other? [_ eid]
                      "returns true if there is some occupying body with center-tile = this cell
                      or a multiple-cell-size body which touches this cell.")
  (nearest-entity          [_ faction])
  (nearest-entity-distance [_ faction])
  (pf-blocked? [_]))

(defrecord FieldData [distance eid])

(defn add-field-data [cell faction distance eid]
  (assoc cell faction (->FieldData distance eid)))

(defn remove-field-data [cell faction]
  (assoc cell faction nil)) ; don't dissoc - will lose the Cell record type

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
    (-> this faction :distance))

  (pf-blocked? [this]
    (blocked? this :z-order/ground)))

(defn create [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (utils/tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))
