(ns gdl.graphics.g2d.batch)

(defprotocol Batch
  (set-color! [_ color])
  (set-projection-matrix! [_ matrix])
  (begin! [_])
  (end! [_])
  (draw! [_ texture-region [x y] [w h] rotation]))
