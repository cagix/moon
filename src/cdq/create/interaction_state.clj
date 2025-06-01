(ns cdq.create.interaction-state
  (:require [cdq.ctx :as ctx]
            [cdq.g.interaction-state]))

(defn do! [ctx]
  (extend (class ctx)
    ctx/InteractionState
    {:interaction-state cdq.g.interaction-state/create})
  ctx)
