(ns cdq.font)

(defprotocol Font
  (draw-text! [_ batch {:keys [scale x y text h-align up?]}]))
