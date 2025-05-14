(ns cdq.game)

(defn- execute!* [transaction]
  (try
   (let [[tx-name params] (if (symbol? transaction)
                            [transaction nil]
                            transaction)
         do! (requiring-resolve (symbol (str "cdq.game." tx-name "/do!")))]
     (if params
       (do! params)
       (do!)))
   (catch Throwable t
     (throw (ex-info "execute!*"
                     {:transaction transaction}
                     t)))))

(defn execute! [transactions]
  (run! execute!* transactions))
