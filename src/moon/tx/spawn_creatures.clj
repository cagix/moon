(ns moon.tx.spawn-creatures
  (:require [gdl.tiled :as tiled]
            [gdl.utils :refer [tile->middle]]
            [moon.component :refer [defc] :as component]))

(def ^:private ^:dbg-flag spawn-enemies? true)

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defc :tx/spawn-creatures
  (component/handle [[_ {:keys [tiled-map start-position]}]]
    (for [creature (cons {:position start-position
                          :creature-id :creatures/vampire
                          :components {:entity/fsm {:fsm :fsms/player
                                                    :initial-state :player-idle}
                                       :entity/faction :good
                                       :entity/player? true
                                       :entity/free-skill-points 3
                                       :entity/clickable {:type :clickable/player}
                                       :entity/click-distance-tiles 1.5}}
                         (when spawn-enemies?
                           (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                             {:position position
                              :creature-id (keyword creature-id)
                              :components {:entity/fsm {:fsm :fsms/npc
                                                        :initial-state :npc-sleeping}
                                           :entity/faction :evil}})))]
      [:tx/creature (update creature :position tile->middle)])))
