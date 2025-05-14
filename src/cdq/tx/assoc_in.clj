(ns cdq.tx.assoc-in)

(defn do! [eid ks value]
  (swap! eid assoc-in ks value))
