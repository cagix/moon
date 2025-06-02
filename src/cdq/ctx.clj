(ns cdq.ctx)

(defprotocol Game
  (reset-game-state! [_ world-fn]))

(defprotocol EffectHandler
  (handle-txs! [_ transactions]))

(defprotocol InteractionState
  (interaction-state [_ eid]))

(defprotocol EffectContext
  (player-effect-ctx [_ eid])
  (npc-effect-ctx [_ eid]))

(defprotocol InfoText
  (info-text [_ object]))

(defprotocol TileColorSetter
  (tile-color-setter [_]))

(defprotocol Context
  (context-entity-add! [_ eid])
  (context-entity-remove! [_ eid])
  (context-entity-moved! [_ eid]))

(defprotocol LineOfSight
  (line-of-sight? [_ source target]))

(defprotocol Grid
  (nearest-enemy-distance [_ entity])
  (nearest-enemy [_ entity])
  (potential-field-find-direction [_ eid]))

(defprotocol SpawnEntity
  (spawn-entity! [_ position body components]))

(defprotocol SpawnCreature
  (spawn-creature! [_ {:keys [position creature-id components]}]))

(defprotocol Graphics
  (handle-draws! [_ draws])
  (sprite [_ texture-path])
  (sub-sprite [_ sprite [x y w h]])
  (sprite-sheet [_ texture-path tilew tileh])
  (sprite-sheet->sprite [_ sprite [x y]]))
