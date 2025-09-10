(ns cdq.start.info
  (:require cdq.effects))

(defn do! [ctx]
  (update ctx :ctx/info cdq.effects/walk-method-map))
