(ns cdq.tx.mod-remove
  (:require [cdq.entity :as entity]))

(defn do! [eid modifiers]
  (swap! eid entity/mod-remove modifiers))
