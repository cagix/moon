(ns cdq.application.render.remove-interaction-state)

(defn do! [ctx]
  (dissoc ctx :ctx/interaction-state))
