(ns cdq.create.world-unit-scale)

(def tile-size 48)

(defn do! [ctx]
  (assoc ctx :ctx/world-unit-scale (float (/ tile-size))))
