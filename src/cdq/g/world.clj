(ns cdq.g.world
  (:require [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [clojure.data.grid2d :as g2d]
            [clojure.gdx.tiled :as tiled]
            [clojure.utils :as utils]))

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
                  active-entities
                  paused?]
  world/World
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
                 :active-entities nil
                 :paused? nil})))
