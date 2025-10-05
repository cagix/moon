(ns cdq.graphics.camera)

(defprotocol Camera
  (position [_])
  (visible-tiles [_])
  (frustum [_])
  (zoom [_])
  (change-zoom! [_ amount])
  (set-position! [_ position]))
