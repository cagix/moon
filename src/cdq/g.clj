(ns cdq.g)
; TODO cdq.graphics always as 'g'
; and 'g' as 'world' ?!?

(defprotocol EffectHandler
  (handle-txs! [_ transactions]))

(defprotocol InteractionState
  (interaction-state [_ eid]))

(defprotocol EffectContext
  (player-effect-ctx [_ eid])
  (npc-effect-ctx [_ eid]))
