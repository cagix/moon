(ns cdq.start.entity-states
  (:require [cdq.effects]))

(defn do! [ctx]
  (update ctx :ctx/entity-states cdq.effects/walk-method-map))
