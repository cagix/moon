(ns cdq.world.entities
  (:require [clojure.tiled :as tiled]
            [clojure.utils :refer [tile->middle]]
            [clojure.world :refer [spawn-creature]]))

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

(defn- spawn-enemies! [{:keys [clojure.context/tiled-map] :as c}]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position tile->middle)))
  :ok)

(defn spawn [{:keys [clojure.context/level
                     clojure.context/tiled-map]
              :as context}]
  (spawn-enemies! context)
  (spawn-creature context
                  (player-entity-props (:start-position level))))
