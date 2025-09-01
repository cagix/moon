(ns cdq.start.bind-root)

(defn do! [[var-sym value]]
  (.bindRoot (requiring-resolve var-sym) value))
