(ns cdq.start.txs
  (:require [cdq.ctx :as ctx]
            [cdq.fn-map :as fn-map]
            [clojure.action-handler :as action-handler]))

(defn do! [ctx fn-map-decl]
  (let [txs-fn-map (fn-map/create fn-map-decl)]
    (extend-type (class ctx)
      ctx/TransactionHandler
      (handle-txs! [ctx transactions]
        (action-handler/handle-txs! txs-fn-map
                                    ctx
                                    transactions))))
  ctx)
