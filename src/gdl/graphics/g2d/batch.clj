(ns gdl.graphics.g2d.batch)

(defprotocol Batch
  (set-color! [_ color])
  (set-projection-matrix! [_ matrix])
  (begin! [_])
  (end! [_])
  (draw! [_ texture-region {:keys [x y origin-x origin-y w h scale-x scale-y rotation]}]))
