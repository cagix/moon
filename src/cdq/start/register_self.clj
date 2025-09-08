(ns cdq.start.register-self)

(defn do! [ctx]
  (assoc ctx :ctx/application-state @(requiring-resolve (:cdq.start.register-self (:ctx/config ctx)))))
