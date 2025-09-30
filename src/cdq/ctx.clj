(ns cdq.ctx)

(defprotocol TransactionHandler
  (handle-txs! [_ transactions]))

(defprotocol ResetGameState
  (reset-game-state! [_ world-fn]))
