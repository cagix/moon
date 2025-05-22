(ns cdq.g)

(defrecord Game [])

(defprotocol World
  (spawn-entity! [_ position body components])
  (line-of-sight? [_ source target])
  (path-blocked? [_ start end width])
  (potential-field-find-direction [_ eid])
  (nearest-enemy-distance [_ entity])
  (player-movement-vector [_]))
