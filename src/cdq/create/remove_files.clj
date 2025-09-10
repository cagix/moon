(ns cdq.create.remove-files)

(defn do! [ctx]
  (dissoc ctx :ctx/files))
