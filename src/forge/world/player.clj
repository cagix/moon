(ns forge.world.player
  (:require [forge.utils :refer [tile->middle
                                 bind-root]]
            [forge.world :refer [spawn-creature]]))

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

(declare player-eid)

(defn init [start-position]
  (bind-root player-eid
             (spawn-creature (player-entity-props start-position))))
