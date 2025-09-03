(ns cdq.tx.assoc)

(defn do! [[_ eid k value] _ctx]
  (swap! eid assoc k value)
  nil)
