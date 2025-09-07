(ns cdq.core)

(defn execute! [[f params]]
  ((requiring-resolve f) params))
