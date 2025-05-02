(ns cdq.world.context
  (:require [cdq.context :as context]
            [cdq.level :as level]
            [cdq.stage]
            [cdq.grid :as grid]
            [cdq.world :refer [spawn-creature]]
            [cdq.data.grid2d :as g2d]
            [cdq.tiled :as tiled]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils :refer [tile->middle defcomponent]])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

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
    :middle (tile->middle position)
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

(defn- raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell grid/blocks-vision?))
    [arr width height]))

(defn- create-content-grid* [{:keys [cell-size width height]}]
  {:grid (g2d/create-grid
          (inc (int (/ width  cell-size))) ; inc because corners
          (inc (int (/ height cell-size)))
          (fn [idx]
            (atom {:idx idx,
                   :entities #{}})))
   :cell-w cell-size
   :cell-h cell-size})

(defn- content-grid [tiled-map]
  (create-content-grid* {:cell-size 16
                         :width  (tiled/tm-width  tiled-map)
                         :height (tiled/tm-height tiled-map)}))

(defn- player-entity-props [start-position]
  {:position (utils/tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- spawn-enemies! [{:keys [cdq.context/tiled-map] :as c}]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position utils/tile->middle)))
  :ok)

(defn- spawn-creatures! [{:keys [cdq.context/level
                                 cdq.context/tiled-map]
                          :as context}]
  (spawn-enemies! context)
  (spawn-creature context
                  (player-entity-props (:start-position level))))

(defn- reset-stage! [{:keys [cdq.context/stage] :as context}]
  (Stage/.clear stage)
  (run! #(stage/add-actor stage %) (cdq.stage/actors context)))

(defn reset [context {:keys [world-id] :as _config}]
  (reset-stage! context)
  (let [{:keys [tiled-map start-position] :as level} (level/create context world-id)
        grid (create-grid tiled-map)
        context (merge context
                       {:cdq.context/content-grid (content-grid tiled-map)
                        :cdq.context/elapsed-time 0
                        :cdq.context/entity-ids (atom {})
                        :cdq.context/player-message (atom {:duration-seconds 1.5})
                        :cdq.context/level level
                        :cdq.context/error nil
                        :cdq.context/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                                  (tiled/tm-height tiled-map)
                                                                                  (constantly false)))
                        :cdq.context/grid grid
                        :cdq.context/tiled-map tiled-map
                        :cdq.context/raycaster (raycaster grid)
                        :cdq.context/factions-iterations {:good 15 :evil 5}
                        :world/potential-field-cache (atom nil)})]
    (assoc context :cdq.context/player-eid (spawn-creatures! context))))

(defcomponent :cdq.context/entity-ids
  (context/add-entity [[_ entity-ids] eid]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid)))

  (context/remove-entity [[_ entity-ids] eid]
    (let [id (:entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))))

(defcomponent :cdq.context/content-grid
  (context/add-entity [[_ {:keys [grid cell-w cell-h]}] eid]
    (let [{:keys [cdq.content-grid/content-cell] :as entity} @eid
          [x y] (:position entity)
          new-cell (get grid [(int (/ x cell-w))
                              (int (/ y cell-h))])]
      (when-not (= content-cell new-cell)
        (swap! new-cell update :entities conj eid)
        (swap! eid assoc :cdq.content-grid/content-cell new-cell)
        (when content-cell
          (swap! content-cell update :entities disj eid)))))

  (context/remove-entity [_ eid]
    (-> @eid
        :cdq.content-grid/content-cell
        (swap! update :entities disj eid)))

  (context/position-changed [this eid]
    (context/add-entity this eid)))

(defcomponent :cdq.context/grid
  (context/add-entity [[_ grid] eid]
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (set-cells! grid eid)
    (when (:collides? @eid)
      (set-occupied-cells! grid eid)))

  (context/remove-entity [[_ _grid] eid]
    (remove-from-cells! eid)
    (when (:collides? @eid)
      (remove-from-occupied-cells! eid)))

  (context/position-changed [[_ grid] eid]
    (remove-from-cells! eid)
    (set-cells! grid eid)
    (when (:collides? @eid)
      (remove-from-occupied-cells! eid)
      (set-occupied-cells! grid eid))))
