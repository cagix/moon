(ns cdq.start.entity-components
  (:require [cdq.effects]))

(defn do! [ctx]
  (update ctx :ctx/entity-components cdq.effects/walk-method-map))
