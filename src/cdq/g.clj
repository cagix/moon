(ns cdq.g)

(defprotocol Config
  (config [_ key]))

(defprotocol ActiveEntities
  (get-active-entities [_]))

(defprotocol Graphics
  (draw-world-map! [_]))

(defprotocol Database
  (get-raw [_ property-id])
  (build [_ property-id])
  (build-all [_ property-type])
  (property-types [_])
  (schemas [_])
  (update-property! [_ property])
  (delete-property! [_ property-id]))

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
