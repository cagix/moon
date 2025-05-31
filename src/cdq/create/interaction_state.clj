(ns cdq.create.interaction-state
  (:require [cdq.g :as g]
            [cdq.g.interaction-state]))

(defn do! [ctx]
  (extend (class ctx)
    g/InteractionState
    {:interaction-state cdq.g.interaction-state/create})
  ctx)
