(ns cdq.start.txs
  (:require [cdq.ctx :as ctx]))

(defn- create-fn-map [{:keys [ks sym-format]}]
  (into {}
        (for [k ks
              :let [sym (symbol (format sym-format (name k)))
                    f (requiring-resolve sym)]]
          (do
           (assert f (str "Cannot resolve " sym))
           [k f]))))

(defn actions!
  [txs-fn-map ctx transactions]
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

(defn do! [ctx fn-map-decl]
  (let [txs-fn-map (create-fn-map fn-map-decl)]
    (extend-type (class ctx)
      ctx/TransactionHandler
      (handle-txs! [ctx transactions]
        (actions! txs-fn-map
                  ctx
                  transactions))))
  ctx)
