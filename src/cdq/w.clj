(ns cdq.w)

(defprotocol World
  (line-of-sight? [_ source target])
  (nearest-enemy-distance [_ entity])
  (nearest-enemy [_ entity])
  (potential-field-find-direction [_ eid])
  (creatures-in-los-of-player [_])
  (npc-effect-ctx [_ eid])
  (path-blocked? [_ start end width])
  (tick-potential-fields! [_])
  (update-time [_ delta-ms]))
