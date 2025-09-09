(ns cdq.tx.dissoc)

(defn do! [[_ eid k] _ctx]
  (swap! eid dissoc k)
  nil)
