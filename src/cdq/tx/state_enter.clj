(ns cdq.tx.state-enter)

(defn do!
  [{:keys [ctx/entity-states]}
   eid
   [state-k state-v]]
  (when-let [f (state-k (:enter entity-states))]
    (f state-v eid)))
