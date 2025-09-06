(ns cdq.create.world-unit-scale)

(defn do! [ctx]
  (assoc ctx :ctx/world-unit-scale (float (/ (:tile-size (:ctx/config ctx))))))
