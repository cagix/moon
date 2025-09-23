(ns cdq.input)

(defprotocol Input
  (player-movement-vector [_])
  (zoom-in? [input])
  (zoom-out? [input])
  (close-windows? [input])
  (toggle-inventory? [input])
  (toggle-entity-info? [input])
  (unpause? [input])
  (open-debug-button-pressed? [input])
  (mouse-position [input]))
