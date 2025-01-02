(ns gdl.context.world-unit-scale)

(defn create [[_ tile-size] _c]
  (float (/ tile-size)))
