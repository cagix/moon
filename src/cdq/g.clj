(ns cdq.g)

(defprotocol Config
  (config [_ key]))

(defprotocol Context
  (context-entity-add! [_ eid])
  (context-entity-remove! [_ eid])
  (context-entity-moved! [_ eid]))

(defprotocol ActiveEntities
  (get-active-entities [_]))

(defprotocol DrawWorldMap
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

(defprotocol SpawnEntity
  (spawn-entity! [_ position body components]))

(defprotocol Creatures
  (spawn-creature! [_ {:keys [position creature-id components]}]))

(defprotocol Assets
  (sound [_ path])
  (texture [_ path])
  (all-sounds [_])
  (all-textures [_]))

(defprotocol BaseGraphics
  (delta-time [_])
  (set-cursor! [_ cursor])
  (frames-per-second [_])
  (clear-screen! [_]))

(defprotocol WorldViewport
  (world-mouse-position [_])
  (camera-position [_])
  (inc-zoom! [_ amount])
  (camera-frustum [_])
  (visible-tiles [_])
  (camera-zoom [_])
  (world-viewport-width [_])
  (world-viewport-height [_])
  (set-camera-position! [_ position]))

(defprotocol UIViewport
  (ui-mouse-position [_])
  (ui-viewport-width [_])
  (ui-viewport-height [_]))

(defprotocol Graphics
  (draw-on-world-viewport! [_ fns])
  (pixels->world-units [_ pixels])
  (sprite [_ texture-path])
  (sub-sprite [_ sprite [x y w h]])
  (sprite-sheet [_ texture-path tilew tileh])
  (sprite-sheet->sprite [_ sprite [x y]]))

(defprotocol TiledMapRenderer
  (draw-tiled-map! [_ tiled-map color-setter]))

(defprotocol Draws
  (handle-draws! [_ draws]))

(defprotocol Stage
  (get-actor [_ id])
  (find-actor-by-name [_ name])
  (add-actor! [_ actor])
  (mouseover-actor [_])
  (reset-actors! [_ actors])
  (draw-stage! [_])
  (update-stage! [_]))

(defprotocol Input
  (button-just-pressed? [_ button])
  (key-pressed? [_ key])
  (key-just-pressed? [_ key]))
