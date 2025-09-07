(ns cdq.create.config)

(def params
  {:cdq.game/enemy-components {:entity/fsm {:fsm :fsms/npc
                                            :initial-state :npc-sleeping}
                               :entity/faction :evil}
   :cdq.game/player-props {:creature-id :creatures/vampire
                           :components {:entity/fsm {:fsm :fsms/player
                                                     :initial-state :player-idle}
                                        :entity/faction :good
                                        :entity/player? true
                                        :entity/free-skill-points 3
                                        :entity/clickable {:type :clickable/player}
                                        :entity/click-distance-tiles 1.5}}
   :cdq.reset-game-state/world {:content-grid-cell-size 16
                                :potential-field-factions-iterations {:good 15
                                                                      :evil 5}}
   :effect-body-props {:width 0.5
                       :height 0.5
                       :z-order :z-order/effect}

   :controls {:zoom-in :minus
              :zoom-out :equals
              :unpause-once :p
              :unpause-continously :space}})

(defn do! [ctx]
  (assoc ctx :ctx/config params))
