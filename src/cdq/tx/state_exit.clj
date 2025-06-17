(ns cdq.tx.state-exit)

(defn do! [[_ eid [state-k state-v]]
           {:keys [ctx/world] :as ctx}]
  (when-let [f (state-k (:state->exit (:world/entity-states world)))]
    (f state-v eid ctx)))
