(ns cdq.tx.set-movement
  (:require [cdq.entity :as entity]))

(defn do! [[_ eid movement-vector] _ctx]
  (swap! eid entity/set-movement movement-vector)
  nil)
