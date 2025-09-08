(ns cdq.start.pipeline.register-self)

(defn do! [ctx]
  (assoc ctx :ctx/application-state @(requiring-resolve (:ctx/state-atom ctx))))
