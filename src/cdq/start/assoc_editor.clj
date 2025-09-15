(ns cdq.start.assoc-editor)

(defn do! [ctx config]
  (assoc ctx :ctx/editor config))
