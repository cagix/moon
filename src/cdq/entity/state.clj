(ns cdq.entity.state)

(declare ->create
         state->enter
         state->cursor
         state->exit
         state->handle-input)

(defn create [ctx state-k eid params]
  {:pre [(keyword? state-k)]}
  (let [result (if-let [f (state-k ->create)]
                 (f eid params ctx)
                 (if params
                   params
                   :something ; nil components are not tick'ed1
                   ))]
    #_(binding [*print-level* 2]
        (println "result of create-state-v " state-k)
        (clojure.pprint/pprint result))
    result))


