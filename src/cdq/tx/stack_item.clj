(ns cdq.tx.stack-item
  (:require [cdq.tx :as tx]))

(defn do! [eid cell item]
  (tx/stack-item eid cell item))
