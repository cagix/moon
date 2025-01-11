(ns cdq.create
  (:require [gdl.utils :refer [safe-merge tile->middle]]
            [gdl.grid2d :as g2d]
            [gdl.tiled :as tiled]
            [cdq.context :refer [spawn-creature mouseover-entity]]
            [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]))

(defn- create-content-grid [tiled-map {:keys [cell-size]}]
  (content-grid/create {:cell-size cell-size
                        :width  (tiled/tm-width  tiled-map)
                        :height (tiled/tm-height tiled-map)}))

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

(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- spawn-player-entity [context start-position]
  (spawn-creature context (player-entity-props start-position)))

(defn- spawn-enemies! [{:keys [cdq.context/tiled-map] :as c}]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position tile->middle)))
  c)

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- create-raycaster [{:keys [cdq.context/grid] :as context}]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell grid/blocks-vision?))
    (assoc context :cdq.context/raycaster [arr width height])))

(defn- explored-tile-corners* [{:keys [cdq.context/tiled-map] :as context}]
  (assoc context :cdq.context/explored-tile-corners
         (atom (g2d/create-grid
                (tiled/tm-width  tiled-map)
                (tiled/tm-height tiled-map)
                (constantly false)))))

(defn error* [context]
  (assoc context :cdq.context/error nil))

(defn tiled-map* [context]
  (assoc context :cdq.context/tiled-map (:tiled-map (:cdq.context/level context))))

(defn grid* [context]
  (assoc context :cdq.context/grid (create-grid (:cdq.context/tiled-map context))))

(defn content-grid* [context]
  (assoc context :cdq.context/content-grid
         (create-content-grid (:cdq.context/tiled-map context)
                              (:content-grid (:gdl/config context)))))

(defn entity-ids* [context]
  (assoc context :cdq.context/entity-ids (atom {})))

(defn factions-iterations* [{:keys [gdl/config] :as context}]
  (assoc context :cdq.context/factions-iterations (:factions-iterations config)))

(defn player-eid* [context]
  (assoc context :cdq.context/player-eid (spawn-player-entity context (:start-position (:cdq.context/level context)))))
