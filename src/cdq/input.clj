(ns cdq.input)

(defprotocol Input
  (player-movement-vector [_])
  (zoom-in? [_])
  (zoom-out? [_])
  (close-windows? [_])
  (toggle-inventory? [_])
  (toggle-entity-info? [_])
  (unpause? [_])
  (open-debug-button-pressed? [_])
  (mouse-position [_]))
