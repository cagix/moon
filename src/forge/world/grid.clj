(ns forge.world.grid
  (:require [clojure.gdx.tiled :as tiled]
            [data.grid2d :as g2d]
            [forge.utils :refer [bind-root tile->middle]]))

(defprotocol Cell
  (cell-blocked? [cell* z-order])
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
  Cell
  (cell-blocked? [_ z-order]
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

(defn- ->cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(declare world-grid)

(defn init [tiled-map]
  (bind-root world-grid (g2d/create-grid
                         (tiled/tm-width tiled-map)
                         (tiled/tm-height tiled-map)
                         (fn [position]
                           (atom (->cell position
                                         (case (tiled/movement-property tiled-map position)
                                           "none" :none
                                           "air"  :air
                                           "all"  :all)))))))
