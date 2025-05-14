(ns cdq.tx.mod-add
  (:require [cdq.entity :as entity]))

(defn do! [eid modifiers]
  (swap! eid entity/mod-add modifiers))
