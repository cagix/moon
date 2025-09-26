(ns cdq.tx.mod-add
  (:require [cdq.stats :as stats]))

(defn do! [_ctx eid modifiers]
  (swap! eid update :creature/stats stats/add modifiers)
  nil)
