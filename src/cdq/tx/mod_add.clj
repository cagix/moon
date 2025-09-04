(ns cdq.tx.mod-add
  (:require [cdq.stats :as modifiers]))

(defn do! [[_ eid modifiers] _ctx]
  (swap! eid update :creature/stats modifiers/add modifiers)
  nil)
