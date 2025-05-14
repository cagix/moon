(ns cdq.tx.mark-destroyed
  (:require [cdq.entity :as entity]))

(defn do! [eid]
  (swap! eid entity/mark-destroyed))
