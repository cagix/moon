(ns cdq.ctx)

(defprotocol TxHandler
  (handle-txs! [_ txs]))

(defprotocol Game
  (reset-game-state! [_]))

(defprotocol EffectContext
  (player-effect-ctx [_ eid]))

(defprotocol InfoText
  (info-text [_ object]))

(defprotocol Editor
  (open-property-editor-window! [_ property])
  (open-editor-overview-window! [_ property-type]))
