(ns cdq.ctx.game
  (:require [cdq.db :as db]
            [gdl.ui.stage :as stage]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.world :as world]
            [master.yoda :as yoda]))

(defn- generate-level [{:keys [ctx/db] :as ctx} world-fn]
  (let [{:keys [tiled-map
                start-position] :as level} (let [[f params] world-fn]
                                             (f ctx params))
        enemy-components (:cdq.ctx.game/enemy-components (:ctx/config ctx))
        creatures-to-spawn (for [[position creature-id] (tiled/positions-with-property tiled-map "creatures" "id")]
                             {:position (utils/tile->middle position)
                              :creature-property (db/build db (keyword creature-id))
                              :components enemy-components})
        {:keys [creature-id
                components]} (:cdq.ctx.game/player-props (:ctx/config ctx))
        player-entity {:position (utils/tile->middle start-position)
                       :creature-property (db/build db creature-id)
                       :components components}]
    (assoc level
           :creatures creatures-to-spawn
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
