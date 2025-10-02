(ns gdl.txs)

(defprotocol TransactionHandler
  (handle! [_ txs]))
