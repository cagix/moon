(ns cdq.tx.pickup-item
  (:require [cdq.tx :as tx]))

(defn do! [eid item]
  (tx/pickup-item eid item))
