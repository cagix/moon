(ns cdq.tx.state-enter)

(defn do! [[_ eid [state-k state-v]]
           {:keys [ctx/world] :as ctx}]
  (when-let [f (state-k (:state->enter (:world/entity-states world)))]
    (f state-v eid)))
