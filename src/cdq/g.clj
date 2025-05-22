(ns cdq.g)

(defrecord Game [])

(defprotocol Schema
  (validate [_]))

(defprotocol SpawnEntity
  (spawn-entity! [_ position body components]))

(defprotocol LineOfSight
  (line-of-sight? [_ source target]))

(defprotocol Raycaster
  (ray-blocked? [_ start end])
  (path-blocked? [_ start end width]))

(defprotocol PotentialField
  (potential-field-find-direction [_ eid]))

(defprotocol Grid
  (nearest-enemy-distance [_ entity]))

(defprotocol PlayerMovementInput
  (player-movement-vector [_]))

(defprotocol EffectHandler
  (handle-txs! [_ transactions]))

(defprotocol InteractionState
  (interaction-state [_ eid]))
