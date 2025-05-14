(ns cdq.tx.dissoc)

(defn do! [eid k]
  (swap! eid dissoc k))
