(ns cdq.ctx)

(defprotocol TransactionHandler
  (handle-txs! [_ transactions]))

(defprotocol Validation
  (validate [_]))

(defprotocol InfoText
  (info-text [_ entity]))
