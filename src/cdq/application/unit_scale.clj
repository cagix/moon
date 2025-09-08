(ns cdq.application.unit-scale)

(defn create [ctx]
  (assoc ctx :ctx/unit-scale (atom 1)))
