(ns cdq.ctx)

(defprotocol InteractionState
  (interaction-state [_ player-eid])) ; WTF # 1
