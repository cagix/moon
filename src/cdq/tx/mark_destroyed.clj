(ns cdq.tx.mark-destroyed)

(defn do! [_ctx eid]
  (swap! eid assoc :entity/destroyed? true))
