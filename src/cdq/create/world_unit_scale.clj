(ns cdq.create.world-unit-scale)

(defn do! [_ctx {:keys [tile-size]}]
  (float (/ tile-size)))
