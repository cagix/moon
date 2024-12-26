(ns anvil.world.create
  (:require [anvil.level :refer [generate-level]]
            [cdq.context :as world]
            [cdq.grid :as grid]
            [gdl.stage :as stage]
            [anvil.world.content-grid :as content-grid]
            [data.grid2d :as g2d]
            [gdl.context :as c]
            [gdl.tiled :as tiled]))

(defn-impl world/dispose [{:keys [cdq.context/tiled-map]}]
  (when tiled-map
    (tiled/dispose tiled-map)))

(def ^:private ^:dbg-flag spawn-enemies? true)

(defn- spawn-enemies [c tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (world/creature c (update props :position tile->middle))))

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
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

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- ->raycaster [grid position->blocked?]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell position->blocked?))
    [arr width height]))

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
    (some #(not= % eid) occupied)) ; contains? faster?

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- ->world-grid [tiled-map]
  (g2d/create-grid
   (tiled/tm-width tiled-map)
   (tiled/tm-height tiled-map)
   (fn [position]
     (atom (grid-cell position
                      (case (tiled/movement-property tiled-map position)
                        "none" :none
                        "air"  :air
                        "all"  :all))))))

(defn- ->explored-tile-corners [tiled-map]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))

(defn- ->content-grid [tiled-map]
  (content-grid/create {:cell-size 16  ; FIXME global config
                        :width  (tiled/tm-width  tiled-map)
                        :height (tiled/tm-height tiled-map)}))

(defn- world-init [c {:keys [tiled-map start-position]}]
  (bind-root world/tiled-map tiled-map)
  (bind-root world/explored-tile-corners (->explored-tile-corners tiled-map))
  (bind-root world/grid                  (->world-grid            tiled-map))
  (bind-root world/content-grid          (->content-grid          tiled-map))
  (bind-root world/entity-ids {})
  (bind-root world/raycaster (->raycaster @#'world/grid grid/blocks-vision?))
  (bind-root world/elapsed-time 0)
  (bind-root world/delta-time nil)
  (bind-root world/player-eid (world/creature c (player-entity-props start-position)))
  (when spawn-enemies?
    (spawn-enemies c tiled-map))
  (bind-root world/mouseover-eid nil))

(defn-impl world/create [c world-id]
  ; TODO assert is :screens/world
  (stage/reset (world/widgets c))
  (world/dispose c) ; TODO ... call here?
  (bind-root world/error nil)
  ; generate level -> creates actually the tiled-map and
  ; start-position?
  ; other stuff just depend on it?!
  (world-init c
              (generate-level (c/build c world-id))))
