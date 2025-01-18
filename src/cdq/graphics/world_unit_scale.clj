(ns cdq.graphics.world-unit-scale)

(defn create [tile-size _context]
  (float (/ tile-size)))
