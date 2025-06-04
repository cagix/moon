(ns cdq.create.world-unit-scale)

(defn do! [ctx {:keys [tile-size]}]
  (assoc ctx :ctx/world-unit-scale (float (/ tile-size))))
