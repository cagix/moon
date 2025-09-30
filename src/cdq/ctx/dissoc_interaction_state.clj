(ns cdq.ctx.dissoc-interaction-state)

(defn do! [ctx]
  (dissoc ctx :ctx/interaction-state))
