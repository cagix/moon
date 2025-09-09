(ns cdq.tx.mark-destroyed)

(defn do! [[_ eid] _ctx]
  (swap! eid assoc :entity/destroyed? true)
  nil)
