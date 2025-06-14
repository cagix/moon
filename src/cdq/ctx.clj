(ns cdq.ctx)

(defprotocol Game
  (reset-game-state! [_ world-fn]))

(defprotocol EffectContext
  (player-effect-ctx [_ eid])
  (npc-effect-ctx [_ eid]))

(defprotocol InfoText
  (info-text [_ object]))

(defprotocol LineOfSight
  (line-of-sight? [_ source target]))

(defprotocol Grid
  (nearest-enemy-distance [_ entity])
  (nearest-enemy [_ entity])
  (potential-field-find-direction [_ eid]))

(defprotocol Editor
  (open-property-editor-window! [_ property])
  (open-editor-overview-window! [_ property-type]))

(defprotocol World
  (creatures-in-los-of-player [_]))
