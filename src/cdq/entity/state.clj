(ns cdq.entity.state)

(defn create
  [{:keys [ctx/entity-states]
    :as ctx} state-k eid params]
  {:pre [(keyword? state-k)]}
  (let [result (if-let [f (state-k (:create entity-states))]
                 (f eid params ctx)
                 (if params
                   params
                   :something ; nil components are not tick'ed1
                   ))]
    #_(binding [*print-level* 2]
        (println "result of create-state-v " state-k)
        (clojure.pprint/pprint result))
    result))
