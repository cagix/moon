(ns cdq.start.pipeline.world-unit-scale)

(defn do! [ctx]
  (assoc ctx :ctx/world-unit-scale (float (/ 48))))
