(ns cdq.g)

(defrecord Game [])

(defprotocol SpawnEntity
  (spawn-entity! [_ position body components]))

(defprotocol LineOfSight
  (line-of-sight? [_ source target]))

(defprotocol World
  (path-blocked? [_ start end width])
  (potential-field-find-direction [_ eid])
  (nearest-enemy-distance [_ entity]))

(defprotocol PlayerMovementInput
  (player-movement-vector [_]))

(defprotocol EffectHandler
  (handle-txs! [_ transactions]))

(defprotocol InteractionState
  (interaction-state [_ eid]))
