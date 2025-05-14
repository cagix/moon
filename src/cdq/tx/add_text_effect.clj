(ns cdq.tx.add-text-effect
  (:require [cdq.entity :as entity]))

(defn do! [eid text]
  (swap! eid entity/add-text-effect text))
