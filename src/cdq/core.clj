(ns cdq.core)

(defn dispatch!
  [{:keys [on
           k->executions]}]
  (doseq [[f params] (k->executions ((requiring-resolve on)))]
    ((requiring-resolve f) params)))
