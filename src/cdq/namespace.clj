(ns cdq.namespace)

(defn require-all! [namespaces]
  (run! require namespaces))
