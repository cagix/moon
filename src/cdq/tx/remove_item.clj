(ns cdq.tx.remove-item
  (:require [cdq.tx :as tx]))

(defn do! [eid cell]
  (tx/remove-item eid cell))
