(ns cdq.graphics.world-unit-scale)

(defn create [_context tile-size]
  (float (/ tile-size)))
