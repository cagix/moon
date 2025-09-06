(ns cdq.create.frame)

(defn do! [ctx]
  (assoc ctx
         :ctx/mouseover-eid nil
         :ctx/paused? nil
         :ctx/delta-time 2
         :ctx/active-entities 1))
