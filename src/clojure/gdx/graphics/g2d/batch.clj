(ns clojure.gdx.graphics.g2d.batch)

(defprotocol Batch
  (set-color! [_ color])
  (draw! [_ texture-region {:keys [x y origin-x origin-y width height scale-x scale-y rotation]}])
  (begin! [_])
  (end! [_])
  (set-projection-matrix! [_ matrix]))
