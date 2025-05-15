(ns cdq.tx.set-item
  (:require [cdq.tx :as tx]))

(defn do! [eid cell item]
  (tx/set-item eid cell item))
