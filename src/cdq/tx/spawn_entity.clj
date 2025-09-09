(ns cdq.tx.spawn-entity
  (:require [cdq.world :as world]))

(defn do! [[_ components] ctx]
  (world/spawn-entity! ctx components))
