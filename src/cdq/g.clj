(ns cdq.g)

(defprotocol Game
  (reset-game-state! [_ world-fn]))

(defprotocol Context
  (context-entity-add! [_ eid])
  (context-entity-remove! [_ eid])
  (context-entity-moved! [_ eid]))

(defprotocol StageActors
  (open-error-window! [_ throwable])
  (selected-skill [_]))

(defprotocol LineOfSight
  (line-of-sight? [_ source target]))

(defprotocol Grid
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

(defprotocol InfoText
  (info-text [_ object]))

(defprotocol SpawnEntity
  (spawn-entity! [_ position body components]))

(defprotocol Creatures
  (spawn-creature! [_ {:keys [position creature-id components]}]))

(defprotocol Graphics
  (delta-time [_])
  (frames-per-second [_])
  (clear-screen! [_])
  (handle-draws! [_ draws])
  (draw-on-world-viewport! [_ f])
  (set-camera-position! [_ position])
  (world-viewport-width [_])
  (world-viewport-height [_])
  (camera-position [_])
  (inc-zoom! [_ amount])
  (camera-frustum [_])
  (visible-tiles [_])
  (camera-zoom [_])
  (draw-tiled-map! [_ tiled-map color-setter])
  (set-cursor! [_ cursor])
  (pixels->world-units [_ pixels])
  (sprite [_ texture-path])
  (sub-sprite [_ sprite [x y w h]])
  (sprite-sheet [_ texture-path tilew tileh])
  (sprite-sheet->sprite [_ sprite [x y]]))

(defprotocol Stage
  (mouseover-actor [_]))

(defprotocol MouseViewports
  (world-mouse-position [_])
  (ui-mouse-position [_]))

(defprotocol Assets
  (sound [_ path])
  (texture [_ path])
  (all-sounds [_])
  (all-textures [_]))

(defprotocol EditorWindow
  (open-editor-window! [_ property-type])
  (edit-property! [_ property]))

(defprotocol Database
  (get-raw [_ property-id])
  (update-property [_ property])
  (delete-property [_ property-id]))
