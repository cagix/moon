(ns cdq.ctx.create.dissoc-files)

(defn do! [ctx]
  (dissoc ctx :ctx/files))
