(ns cdq.tx.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.world :as world]
            [cdq.create.world]))

(defn do!
  [{:keys [ctx/application-state]
    :as ctx}
   world-fn]
  (ctx/handle-txs! ctx
                   [[:tx/reset-stage]])
  (world/dispose! (:ctx/world ctx))
  (swap! application-state cdq.create.world/do! world-fn)
  nil)
