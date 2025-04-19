(ns cdq.world.context
  (:require [cdq.create.level :as level]
            cdq.create.grid
            cdq.create.explored-tile-corners
            cdq.create.entity-ids
            cdq.create.potential-fields
            cdq.create.content-grid
            [cdq.grid :as grid]
            [cdq.stage :as stage]
            [cdq.world :refer [spawn-creature]]
            [clojure.data.grid2d :as g2d]
            [clojure.gdx.tiled :as tiled]
            [clojure.utils :as utils]))

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

(def actors
  '[(cdq.create.stage.dev-menu/create (cdq.create.stage.dev-menu.config/create))
    (cdq.create.stage.actionbar/create)
    (cdq.create.stage.hp-mana-bar/create)
    (cdq.create.stage.windows/create [(cdq.create.stage.entity-info-window/create)
                                      (cdq.widgets.inventory/create)])
    (cdq.create.stage.player-state/create)
    (cdq.create.stage.player-message/actor)])

(defn- reset-stage! [{:keys [cdq.context/stage] :as context}]
  (com.badlogic.gdx.scenes.scene2d.Stage/.clear stage)
  (run! #(stage/add-actor stage %)
        (map (fn [fn-invoc]
               (utils/req-resolve-call fn-invoc context))
             actors)))

(defn reset [context world-id]
  (reset-stage! context)
  (let [{:keys [tiled-map start-position] :as level} (level/create context world-id)
        grid (cdq.create.grid/create tiled-map)
        context (merge context
                       {:cdq.context/content-grid (cdq.create.content-grid/create tiled-map)
                        :cdq.context/elapsed-time 0
                        :cdq.context/entity-ids (cdq.create.entity-ids/create)
                        :cdq.context/player-message (atom {:duration-seconds 1.5})
                        :cdq.context/level level
                        :cdq.context/error nil
                        :cdq.context/explored-tile-corners (cdq.create.explored-tile-corners/create tiled-map)
                        :cdq.context/grid grid
                        :cdq.context/tiled-map tiled-map
                        :cdq.context/raycaster (raycaster grid)
                        :cdq.context/factions-iterations {:good 15 :evil 5}
                        :world/potential-field-cache (cdq.create.potential-fields/create)})]
    (assoc context :cdq.context/player-eid (spawn-creatures! context))))
