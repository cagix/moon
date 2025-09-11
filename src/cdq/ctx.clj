(ns cdq.ctx
  (:require [cdq.db :as db]))

(defprotocol TransactionHandler
  (handle-txs! [_ transactions]))

(defn call-world-fn
  [{:keys [ctx/db
           ctx/graphics]}
   [f params]]
  ((requiring-resolve f)
   (assoc params
          :creature-properties (db/all-raw db :properties/creatures)
          :graphics graphics)))

(defn reset-game-state!
  [ctx world-fn]
  (-> ctx
      ((requiring-resolve 'cdq.ctx.call-world-fn/do!) (call-world-fn ctx world-fn))
      ((requiring-resolve 'cdq.ctx.build-world/do!))
      ((requiring-resolve 'cdq.ctx.spawn-player/do!))
      ((requiring-resolve 'cdq.ctx.spawn-enemies/do!))))
