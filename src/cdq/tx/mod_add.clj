(ns cdq.tx.mod-add
  (:require [cdq.world.entity.stats :as modifiers]))

(defn do! [[_ eid modifiers] _ctx]
  (swap! eid update :creature/stats modifiers/add modifiers)
  nil)
