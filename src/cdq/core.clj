(ns cdq.core)

(fn [[f params]]
  ((requiring-resolve f) params))
