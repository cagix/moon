(ns cdq.tx.mod-add
  (:require [cdq.stats :as stats]))

(defn do! [[_ eid modifiers] _ctx]
  (swap! eid update :creature/stats stats/add modifiers)
  nil)
