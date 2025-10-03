(ns cdq.world)

(defprotocol World
  (cache-active-entities [_])
  (update-potential-fields! [_])
  (tick-entities! [_])
  )
