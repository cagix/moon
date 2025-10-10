(ns cdq.game.create.tx-handler
  (:require [clojure.tx-handler :as tx-handler]
            [clojure.txs :as txs]))

(defn do! [ctx
           txs-fn-map
           reaction-txs-fn-map]
  (let [txs-fn-map          (update-vals txs-fn-map          requiring-resolve)
        reaction-txs-fn-map (update-vals reaction-txs-fn-map requiring-resolve)]
    (extend-type (class ctx)
      txs/TransactionHandler
      (handle! [ctx txs]
        (let [handled-txs (tx-handler/actions! txs-fn-map
                                               ctx
                                               txs)]
          (tx-handler/actions! reaction-txs-fn-map
                               ctx
                               handled-txs
                               :strict? false)))))
  ctx)
