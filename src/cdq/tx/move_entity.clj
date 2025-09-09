(ns cdq.tx.move-entity
  (:require [cdq.world :as world]))

(defn do! [params ctx]
  (world/move-entity! ctx params))
