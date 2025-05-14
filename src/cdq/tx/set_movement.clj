(ns cdq.tx.set-movement
  (:require [cdq.entity :as entity]))

(defn do! [eid movement-vector]
  (swap! eid entity/set-movement movement-vector))
