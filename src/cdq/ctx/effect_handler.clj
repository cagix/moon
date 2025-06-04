(ns cdq.ctx.effect-handler)

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defn handle-txs! [ctx transactions]
  (doseq [transaction transactions
          :when transaction]
    (assert (vector? transaction) (pr-str transaction))
    (try (let [result (do! transaction ctx)]
           (if result
             ; (println result)
             ; & _has_ to be handled ...
             )
           )
         (catch Throwable t
           (throw (ex-info "" {:transaction transaction} t))))))
