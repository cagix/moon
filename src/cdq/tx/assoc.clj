(ns cdq.tx.assoc)

(defn do! [eid k value]
  (swap! eid assoc k value))
