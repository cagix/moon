(ns cdq.tx.dissoc)

(defn do! [_ctx eid k]
  (swap! eid dissoc k))
