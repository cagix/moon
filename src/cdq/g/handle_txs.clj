(ns cdq.g.handle-txs
  (:require gdl.application
            [cdq.g]))

(extend-type gdl.application.Context
  cdq.g/EffectHandler
  (handle-txs! [ctx transactions]
    (doseq [transaction transactions
            :when transaction
            :let [_ (assert (vector? transaction)
                            (pr-str transaction))
                  ; TODO also should be with namespace 'tx' the first is a keyword
                  sym (symbol (str "cdq.tx." (name (first transaction)) "/do!"))
                  do! (requiring-resolve sym)]] ; TODO throw error if requiring failes ! compiler errors ... compile all tx/game first ?
      (try (apply do! (cons ctx (rest transaction)))
           (catch Throwable t
             (throw (ex-info ""
                             {:transaction transaction
                              :sym sym}
                             t)))))))
