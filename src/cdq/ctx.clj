(ns cdq.ctx)

(defprotocol TransactionHandler
  (handle-txs! [_ transactions]))

(defprotocol Validation
  (validate [_]))

(defprotocol ResetGameState
  (reset-game-state! [_ world-fn]))

(defprotocol Editor
  (open-editor-overview! [_ {:keys [property-type clicked-id-fn]}]))
