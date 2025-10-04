(ns clojure.txs)

(defprotocol TransactionHandler
  (handle! [_ txs]))
