(ns cdq.game-state.create-world
  (:require [cdq.db :as db]
            [cdq.utils :as utils]
            [cdq.world :as world]))

(defn do!
  [{:keys [ctx/config
           ctx/db] :as ctx}]
  (let [{:keys [tiled-map
                start-position] :as level} (let [[f params] (:config/starting-world config)]
                                             (f ctx params))
        {:keys [creature-id
                components]} (:cdq.ctx.game/player-props (:ctx/config ctx))
        player-entity {:position (utils/tile->middle start-position)
                       :creature-property (db/build db creature-id)
                       :components components}
        ctx (world/create ctx
                          (:cdq.ctx.game/world config)
                          tiled-map
                          player-entity)]

    ctx))
