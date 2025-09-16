(ns cdq.animation)

(defprotocol Animation
  (tick [_ delta])
  (restart [_])
  (stopped? [_])
  (current-frame [_]))
