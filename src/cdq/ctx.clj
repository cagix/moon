(ns cdq.ctx)

(defn handle-txs!
  [{:keys [ctx/txs-fn-map]
    :as ctx}
   transactions]
  (loop [ctx ctx
         transactions transactions
         handled-transactions []]
    (if (seq transactions)
      (let [[k & params :as transaction] (first transactions)]
        (if transaction
          (let [_ (assert (vector? transaction))
                f (get txs-fn-map k)
                new-transactions (try
                                  (apply f ctx params)
                                  (catch Throwable t
                                    (throw (ex-info "Error handling transaction"
                                                    {:transaction transaction}
                                                    t))))]
            (recur ctx
                   (concat (or new-transactions []) (rest transactions))
                   (conj handled-transactions transaction)))
          (recur ctx
                 (rest transactions)
                 handled-transactions)))
      handled-transactions)))
