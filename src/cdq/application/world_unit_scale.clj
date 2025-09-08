(ns cdq.application.world-unit-scale)

(defn create [ctx]
  (assoc ctx :ctx/world-unit-scale (float (/ 48))))
