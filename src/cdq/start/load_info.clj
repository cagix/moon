(ns cdq.start.load-info)

(defn do! [ctx config]
  (assoc ctx :ctx/info config))
