(ns cdq.create.interaction-state
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.interaction-state]))

(defn do! [ctx]
  (extend (class ctx)
    ctx/InteractionState
    {:interaction-state cdq.ctx.interaction-state/interaction-state})
  ctx)
