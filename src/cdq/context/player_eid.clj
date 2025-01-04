(ns cdq.context.player-eid
  (:require [gdl.utils :refer [tile->middle]]
            [cdq.context :refer [spawn-creature]]))

; TODO this passing w. world props ...
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

(defn create [_ {:keys [cdq.context/level] :as c}]
  (assert (:start-position level))
  (spawn-creature c (player-entity-props (:start-position level))))
