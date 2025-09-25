(ns cdq.application.create.remove-plattform)

(defn do! [ctx]
  (dissoc ctx :ctx/plattform))
