(ns cdq.start.register-self)

(defn do! [ctx avar]
  (assoc ctx :ctx/application-state @avar))
