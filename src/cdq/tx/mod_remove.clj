(ns cdq.tx.mod-remove
  (:require [cdq.world.entity.stats :as modifiers]))

(defn do! [[_ eid modifiers] _ctx]
  (swap! eid update :creature/stats modifiers/remove modifiers)
  nil)
