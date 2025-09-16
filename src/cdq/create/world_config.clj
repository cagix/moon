(ns cdq.create.world-config
  (:require [clojure.utils :as utils]
            [malli.core :as m]
            [reduce-fsm :as fsm]))

(comment

 ; 1. quote the data structur ebecause of arrows
 ; 2.
 (eval `(fsm/fsm-inc ~data))
 )

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

(def ^:private components-schema
  (m/schema [:map {:closed true}
             [:entity/body :some]
             [:entity/image {:optional true} :some]
             [:entity/animation {:optional true} :some]
             [:entity/delete-after-animation-stopped? {:optional true} :some]
             [:entity/alert-friendlies-after-duration {:optional true} :some]
             [:entity/line-render {:optional true} :some]
             [:entity/delete-after-duration {:optional true} :some]
             [:entity/destroy-audiovisual {:optional true} :some]
             [:entity/fsm {:optional true} :some]
             [:entity/player? {:optional true} :some]
             [:entity/free-skill-points {:optional true} :some]
             [:entity/click-distance-tiles {:optional true} :some]
             [:entity/clickable {:optional true} :some]
             [:property/id {:optional true} :some]
             [:property/pretty-name {:optional true} :some]
             [:creature/level {:optional true} :some]
             [:entity/faction {:optional true} :some]
             [:entity/species {:optional true} :some]
             [:entity/movement {:optional true} :some]
             [:entity/skills {:optional true} :some]
             [:creature/stats {:optional true} :some]
             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))

(def config
  {:content-grid-cell-size 16
   :potential-field-factions-iterations {:good 15
                                         :evil 5}
   :world/max-delta 0.04
   :world/minimum-size 0.39
   :world/z-orders [:z-order/on-ground
                    :z-order/ground
                    :z-order/flying
                    :z-order/effect]})

(defrecord World [])

(defn do! [ctx]
  (assoc ctx
         :ctx/world
         (merge (map->World {})
                config
                {:world/max-delta    (:world/max-delta    config)
                 :world/minimum-size (:world/minimum-size config)
                 :world/z-orders     (:world/z-orders     config)
                 :world/max-speed (/ (:world/minimum-size config)
                                     (:world/max-delta    config))
                 :world/factions-iterations (:potential-field-factions-iterations config)
                 :world/render-z-order (utils/define-order (:world/z-orders config))
                 :world/spawn-entity-schema components-schema
                 :world/fsms {:fsms/player player-fsm
                              :fsms/npc npc-fsm}
                 :world/enemy-components {:entity/fsm {:fsm :fsms/npc
                                                       :initial-state :npc-sleeping}
                                          :entity/faction :evil}
                 :world/player-components {:creature-id :creatures/vampire
                                           :components {:entity/fsm {:fsm :fsms/player
                                                                     :initial-state :player-idle}
                                                        :entity/faction :good
                                                        :entity/player? true
                                                        :entity/free-skill-points 3
                                                        :entity/clickable {:type :clickable/player}
                                                        :entity/click-distance-tiles 1.5}}
                 :world/effect-body-props {:width 0.5
                                           :height 0.5
                                           :z-order :z-order/effect}})))
