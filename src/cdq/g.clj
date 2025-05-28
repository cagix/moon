(ns cdq.g)

(defprotocol Context
  (context-entity-add! [_ eid])
  (context-entity-remove! [_ eid])
  (context-entity-moved! [_ eid]))

(defprotocol ActiveEntities
  (get-active-entities [_]))

(defprotocol DrawWorldMap
  (draw-world-map! [_]))

(defprotocol StageActors
  (open-error-window! [_ throwable])
  (selected-skill [_]))

(defprotocol LineOfSight
  (line-of-sight? [_ source target]))

(defprotocol Raycaster
  (ray-blocked? [_ start end])
  (path-blocked? [_ start end width]))

(defprotocol Grid
  (valid-position? [_ new-body])
  (grid-cell [_ position])
  (point->entities [_ position])
  (circle->cells [_ circle])
  (circle->entities [_ circle])
  (nearest-enemy-distance [_ entity])
  (nearest-enemy [_ entity])
  (potential-field-find-direction [_ eid]))

(defprotocol PlayerMovementInput
  (player-movement-vector [_]))

(defprotocol EffectHandler
  (handle-txs! [_ transactions]))

(defprotocol InteractionState
  (interaction-state [_ eid]))

(defprotocol EffectContext
  (player-effect-ctx [_ eid])
  (npc-effect-ctx [_ eid]))

(defprotocol Time
  (elapsed-time [_])
  (create-timer [_ duration])
  (timer-stopped? [_ timer])
  (reset-timer [_ timer])
  (timer-ratio [_ timer]))

(defprotocol InfoText
  (info-text [_ object]))

(defprotocol SpawnEntity
  (spawn-entity! [_ position body components]))

(defprotocol Creatures
  (spawn-creature! [_ {:keys [position creature-id components]}]))

(defprotocol BaseGraphics
  (delta-time [_])
  (set-cursor! [_ cursor])
  (frames-per-second [_])
  (clear-screen! [_]))

(defprotocol UIViewport
  (ui-mouse-position [_])
  (ui-viewport-width [_])
  (ui-viewport-height [_]))

(defprotocol Graphics
  (sprite [_ texture-path])
  (sub-sprite [_ sprite [x y w h]])
  (sprite-sheet [_ texture-path tilew tileh])
  (sprite-sheet->sprite [_ sprite [x y]]))

(defprotocol Stage
  (get-actor [_ id])
  (find-actor-by-name [_ name])
  (add-actor! [_ actor])
  (mouseover-actor [_])
  (reset-actors! [_])
  (draw-stage! [_])
  (update-stage! [_]))
