(ns cdq.world)

(defprotocol World
  (spawn-entity! [_ entity])
  (move-entity! [_ [_ eid body direction rotate-in-movement-direction?]])
  (remove-destroyed-entities! [_])
  (creatures-in-los-of [_ entity])
  (assoc-active-entities [_])
  (update-potential-fields! [_])
  (tick-entities! [_]))
