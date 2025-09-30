(ns cdq.ctx.dissoc-files)

(defn do! [ctx]
  (dissoc ctx :ctx/files))
