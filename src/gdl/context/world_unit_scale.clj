(ns gdl.context.world-unit-scale)

(defn create [[_ tile-size] _context]
  (float (/ tile-size)))
