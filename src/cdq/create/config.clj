(ns cdq.create.config)

(defn do! [ctx params]
  (assoc ctx :ctx/config params))
