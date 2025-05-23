(ns cdq.g)

(defrecord Game [])

(defprotocol GameState
  (reset-game-state! [_]))

(defprotocol Stage
  (mouseover-actor [_])
  (selected-skill [_]))

(defprotocol Schema
  (validate [_]))

(defprotocol SpawnEntity
  (spawn-entity! [_ position body components])
  (spawn-effect! [_ position components]))

(defprotocol LineOfSight
  (line-of-sight? [_ source target]))

(defprotocol Raycaster
  (ray-blocked? [_ start end])
  (path-blocked? [_ start end width]))

(defprotocol PotentialField
  (potential-field-find-direction [_ eid]))

(defprotocol Grid
  (nearest-enemy-distance [_ entity])
  (nearest-enemy [_ entity]))

(defprotocol PlayerMovementInput
  (player-movement-vector [_]))

(defprotocol EffectHandler
  (handle-txs! [_ transactions]))

(defprotocol InteractionState
  (interaction-state [_ eid]))

(defprotocol EffectContext
  (player-effect-ctx [_ eid])
  (npc-effect-ctx [_ eid]))

(defprotocol Graphics
  (draw-on-world-viewport! [_ fns])
  (handle-draws! [_ draws]))
