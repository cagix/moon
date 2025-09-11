(ns cdq.tx.state-exit)

(defn do!
  [{:keys [ctx/entity-states]
    :as ctx}
   eid
   [state-k state-v]]
  (when-let [f (state-k (:exit entity-states))]
    (f state-v eid ctx)))
