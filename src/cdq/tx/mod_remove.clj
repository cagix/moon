(ns cdq.tx.mod-remove
  (:require [cdq.stats :as stats]))

(defn do! [_ctx eid modifiers]
  (swap! eid update :entity/stats stats/remove-mods modifiers)
  nil)
