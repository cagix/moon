(ns cdq.tx.mod-add
  (:require [cdq.entity :as entity]))

(defn do! [_ctx eid modifiers]
  (swap! eid entity/mod-add modifiers))
