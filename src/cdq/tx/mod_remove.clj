(ns cdq.tx.mod-remove
  (:require [cdq.stats :as stats]))

(defn do! [[_ eid modifiers] _ctx]
  (swap! eid update :creature/stats stats/remove modifiers)
  nil)
