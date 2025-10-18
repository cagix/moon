(ns cdq.world)

(defprotocol World
  (dispose! [_])
  (cache-active-entities [_])
  (update-potential-fields! [_])
  (tick-entities! [_])
  (remove-destroyed-entities! [_])
  (update-time [_ delta-ms])
  (player-position [_])
  (spawn-entity! [_ entity])
  (mouseover-entity [_ position]))
