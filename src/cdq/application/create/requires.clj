(ns cdq.application.create.requires)

(defn do! [ctx namespaces]
  (run! require namespaces)
  ctx)
