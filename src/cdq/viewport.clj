(ns cdq.viewport)

(defprotocol Viewport
  (update! [_])
  (mouse-position [_]))
