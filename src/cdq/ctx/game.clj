(ns cdq.ctx.game
  (:require [cdq.db :as db]
            [gdl.ui.stage :as stage]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.world :as world]
            [master.yoda :as yoda]))

(defn- tiled-map->creatures-to-spawn [tiled-map]
  (for [[position creature-id] (tiled/positions-with-property tiled-map "creatures" "id")]
    {:position position
     :creature-property (keyword creature-id)}))

(defn- generate-level [{:keys [ctx/db] :as ctx} world-fn]
  (let [{:keys [tiled-map
                start-position] :as level} (let [[f params] world-fn]
                                             (f ctx params))
        enemy-components {:entity/fsm {:fsm :fsms/npc
                                       :initial-state :npc-sleeping}
                          :entity/faction :evil}
        creatures (map (fn [creature]
                         (-> creature
                             (update :position utils/tile->middle)
                             (update :creature-property (partial db/build db))
                             (assoc :components enemy-components)))
                       (tiled-map->creatures-to-spawn tiled-map))
        {:keys [creature-id
                free-skill-points
                click-distance-tiles]} (:cdq.ctx.game/player-props (:ctx/config ctx))
        player-entity {:position (utils/tile->middle start-position)
                       :creature-property (db/build db creature-id)
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

(defn reset-stage! [{:keys [ctx/config
                            ctx/stage]
                     :as ctx}]
  (stage/clear! stage)
  (doseq [[create-actor params] (:cdq.ctx.game/ui-actors config)]
    (stage/add! stage (create-actor ctx params)))
  ctx)

(defn create-world-state [{:keys [ctx/config] :as ctx}]
  (world/create ctx
                (:cdq.ctx.game/world config)
                (generate-level ctx (:config/starting-world config))))

(defn reset-game-state! [{:keys [ctx/config]
                          :as ctx}]
  (reduce yoda/render* ctx (:config/game-state-pipeline config)))
