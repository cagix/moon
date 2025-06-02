(ns clojure.ctx.effect-handler)

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defn handle-txs! [ctx transactions]
  (doseq [transaction transactions
          :when transaction]
    (assert (vector? transaction) (pr-str transaction))
    (try (do! transaction ctx)
         (catch Throwable t
           (throw (ex-info "" {:transaction transaction} t))))))
