(ns cdq.tx.mark-destroyed)

(defn do! [eid]
  (swap! eid assoc :entity/destroyed? true))
