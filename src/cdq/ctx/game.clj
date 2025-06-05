(ns cdq.ctx.game
  (:require [cdq.db :as db]
            [gdl.ui.stage :as stage]
            [cdq.utils :as utils]
            [cdq.world :as world]
            [gdl.tiled :as tiled]))

(defn- generate-level [{:keys [ctx/db] :as ctx} world-fn]
  (let [{:keys [tiled-map
                start-position] :as level} (world-fn ctx)
        creatures (for [[position creature-id] (tiled/positions-with-property tiled-map "creatures" "id")]
                    {:position (utils/tile->middle position)
                     :creature-property (db/build db (keyword creature-id) ctx)
                     :components {:entity/fsm {:fsm :fsms/npc
                                               :initial-state :npc-sleeping}
                                  :entity/faction :evil}})
        {:keys [creature-id
                free-skill-points
                click-distance-tiles]} (:cdq.ctx.game/player-props (:ctx/config ctx))
        player-entity {:position (utils/tile->middle start-position)
                       :creature-property (db/build db creature-id ctx)
                       :components {:entity/fsm {:fsm :fsms/player
                                                 :initial-state :player-idle}
                                    :entity/faction :good
                                    :entity/player? true
                                    :entity/free-skill-points free-skill-points
                                    :entity/clickable {:type :clickable/player}
                                    :entity/click-distance-tiles click-distance-tiles}}]
    (assoc level
           :creatures creatures
           :player-entity player-entity)))

(defn reset-game-state! [{:keys [ctx/config
                                 ctx/stage]
                          :as ctx}
                         world-fn]
  (stage/clear! stage)
  (doseq [create-actor (:cdq.ctx.game/ui-actors config)]
    (stage/add! stage (create-actor ctx)))
  (world/create ctx
                (:cdq.ctx.game/world config)
                (generate-level ctx world-fn)))
