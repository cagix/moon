(ns cdq.g)

(defrecord Game [])

(defprotocol Textures
  (texture [_ path])
  (all-textures [_]))

(defprotocol Sounds
  (sound [_ path])
  (all-sounds [_]))

(defprotocol Config
  (config [_ key]))

(defprotocol Database
  (build [_ property-id])
  (build-all [_ property-type]))

(defprotocol Input
  (button-just-pressed? [_ button])
  (key-pressed? [_ key])
  (key-just-pressed? [_ key]))

(defprotocol GameState
  (reset-game-state! [_]))

(defprotocol Stage
  (mouseover-actor [_])
  (selected-skill [_]))

(defprotocol Schema
  (validate [_]))

; TODO txs should not be available here ....
; but then tx-handler has to know everything again !?
; confused

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

; only missing line of sight and sprites
; set cursor , etc
(defprotocol Graphics
  (draw-on-world-viewport! [_ fns])
  (handle-draws! [_ draws])
  (world-mouse-position [_])
  (ui-mouse-position [_])
  (update-viewports! [_])
  (draw-tiled-map! [_ tiled-map color-setter])
  (camera-position [_])
  (inc-zoom! [_ amount])
  (camera-frustum [_])
  (visible-tiles [_])
  (set-camera-position! [_ position])
  (camera-zoom [_])
  (pixels->world-units [_ pixels]))
