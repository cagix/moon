(ns cdq.start.pipeline.unit-scale)

(defn do! [ctx]
  (assoc ctx :ctx/unit-scale (atom 1)))
