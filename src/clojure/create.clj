(ns clojure.create
  (:require [clojure.utils :refer [safe-merge tile->middle]]
            [clojure.grid2d :as g2d]
            [clojure.tiled :as tiled]
            [clojure.world :refer [spawn-creature]]))

(defn player-entity-props [start-position]
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

(defn spawn-enemies! [{:keys [clojure.context/tiled-map] :as c}]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position tile->middle)))
  :ok)

(defn explored-tile-corners* [{:keys [clojure.context/tiled-map]}]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))

(defn error* [_context]
  nil)

(defn tiled-map* [context]
  (:tiled-map (:clojure.context/level context)))

(defn entity-ids* [_context]
  (atom {}))

(defn factions-iterations* [config _context]
  config)

(defn player-eid* [context]
  (spawn-player-entity context (:start-position (:clojure.context/level context))))
