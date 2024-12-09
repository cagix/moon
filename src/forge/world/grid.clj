(ns forge.world.grid
  (:require [anvil.world :as world]
            [clojure.gdx.tiled :as tiled]
            [clojure.utils :refer [bind-root tile->middle]]
            [data.grid2d :as g2d]))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  world/Cell
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

(defn init [tiled-map]
  (bind-root world/grid (g2d/create-grid
                         (tiled/tm-width tiled-map)
                         (tiled/tm-height tiled-map)
                         (fn [position]
                           (atom (->cell position
                                         (case (tiled/movement-property tiled-map position)
                                           "none" :none
                                           "air"  :air
                                           "all"  :all)))))))
