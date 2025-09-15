(ns cdq.create.assoc-ui-actors)

(defn do! [ctx actor-vec]
  (assoc ctx :ctx/ui-actors actor-vec))
