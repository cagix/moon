(ns cdq.create.unit-scale)

(defn do! [ctx]
  (assoc ctx :ctx/unit-scale (atom 1)))
