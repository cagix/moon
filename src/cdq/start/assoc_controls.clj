(ns cdq.start.assoc-controls)

(defn do! [ctx params]
  (assoc ctx :ctx/controls params))
