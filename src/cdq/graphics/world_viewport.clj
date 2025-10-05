(ns cdq.graphics.world-viewport)

(defprotocol WorldViewport
  (width [_])
  (height [_])
  (unproject [_ position])
  (update! [_ width height])
  (draw! [_ f]))
