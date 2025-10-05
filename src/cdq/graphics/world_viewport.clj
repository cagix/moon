(ns cdq.graphics.world-viewport)

(defprotocol WorldViewport
  (width [_])
  (height [_])
  (unproject [_ position])
  (draw! [_ f]))
