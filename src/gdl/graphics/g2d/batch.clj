(ns gdl.graphics.g2d.batch)

(defprotocol Batch
  (draw-on-viewport! [_ viewport f])
  (draw! [_ texture-region [x y] [w h] rotation]))
