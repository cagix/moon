(ns cdq.game-state.spawn-player
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.utils :as utils]
            [cdq.w :as w]))

(defn do!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (->> (let [{:keys [creature-id
                     components]} (:cdq.ctx.game/player-props config)]
         {:position (utils/tile->middle (:world/start-position world))
          :creature-property (db/build db creature-id)
          :components components})
       (w/spawn-creature! world)
       (ctx/handle-txs! ctx))
  (let [player-eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @player-eid))
    (assoc-in ctx [:ctx/world :world/player-eid] player-eid)))
