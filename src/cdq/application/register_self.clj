(ns cdq.application.register-self)

(defn do! [ctx]
  (assoc ctx :ctx/application-state @(requiring-resolve (:ctx/state-atom ctx))))
