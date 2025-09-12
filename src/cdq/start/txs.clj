(ns cdq.start.txs
  (:require [cdq.ctx]))

(defn- handle-tx!
  [{:keys [ctx/txs-fn-map]
    :as ctx}
   {k 0 :as tx}]
  (apply (get txs-fn-map k) ctx (rest tx)))

(defn- create-fn-map [{:keys [ks sym-format]}]
  (into {}
        (for [k ks
              :let [sym (symbol (format sym-format (name k)))
                    f (requiring-resolve sym)]]
          (do
           (assert f (str "Cannot resolve " sym))
           [k f]))))

(defn do! [ctx]
  (extend-type (class ctx)
    cdq.ctx/TransactionHandler
    (handle-txs! [ctx transactions]
      (loop [ctx ctx
             txs transactions
             handled []]
        (if (seq txs)
          (let [tx (first txs)]
            (if tx
              (let [_ (assert (vector? tx))
                    new-txs (try
                             (handle-tx! ctx tx)
                             (catch Throwable t
                               (throw (ex-info "Error handling transaction" {:transaction tx} t))))]
                (recur ctx
                       (concat (or new-txs []) (rest txs))
                       (conj handled tx)))
              (recur ctx (rest txs) handled)))
          handled))))
  (update ctx :ctx/txs-fn-map create-fn-map))
