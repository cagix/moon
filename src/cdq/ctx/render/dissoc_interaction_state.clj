(ns cdq.ctx.render.dissoc-interaction-state)

(defn do! [ctx]
  (dissoc ctx :ctx/interaction-state))
