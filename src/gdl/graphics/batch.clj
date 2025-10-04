(ns gdl.graphics.batch)

(defprotocol Batch
  (draw! [_ texture-region x y [w h] rotation])
  (set-color! [_ [r g b a]])
  (set-projection-matrix! [_ matrix])
  (begin! [_])
  (end! [_]))
