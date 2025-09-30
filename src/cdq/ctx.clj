(ns cdq.ctx)

(defprotocol TransactionHandler
  (handle-txs! [_ transactions]))
