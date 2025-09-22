(ns cdq.create.requires)

(defn do! [ctx namespaces]
  (run! require namespaces)
  ctx)
