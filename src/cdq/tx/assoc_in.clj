(ns cdq.tx.assoc-in)

(defn do! [[_ eid ks value] _ctx]
  (swap! eid assoc-in ks value)
  nil)
