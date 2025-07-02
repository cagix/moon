(ns cdq.game-state.spawn-enemies
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.w :as w]))

(defn do!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world)
                                                                "creatures"
                                                                "id")]
    (->> {:position (utils/tile->middle position)
          :creature-property (db/build db (keyword creature-id))
          :components (:cdq.ctx.game/enemy-components config)}
         (w/spawn-creature! world)
         (ctx/handle-txs! ctx)))
  ctx)
