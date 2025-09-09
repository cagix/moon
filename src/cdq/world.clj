(ns cdq.world)

(defprotocol World
  (creatures-in-los-of [_ entity])
  (assoc-active-entities [_])
  (update-potential-fields! [_])
  (tick-entities! [_]))
