(ns cdq.tx.mod-add
  (:require [cdq.entity.stats :as stats]))

(defn do! [_ctx eid modifiers]
  (swap! eid update :entity/stats stats/add modifiers)
  nil)
