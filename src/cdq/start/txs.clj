(ns cdq.start.txs
  (:require [cdq.ctx]))

(defn- valid-tx? [transaction]
  (vector? transaction))

(declare txs-fn-map)

(defn- do!*
  [ctx
   {k 0 :as component}]
  (let [f (get txs-fn-map k)]
    (assert f (pr-str k))
    (f component ctx)))

(defn- handle-tx! [ctx tx]
  (assert (valid-tx? tx) (pr-str tx))
  (try
   (do!* ctx tx)
   (catch Throwable t
     (throw (ex-info "Error handling transaction" {:transaction tx} t)))))

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
              (let [new-txs (handle-tx! ctx tx)]
                (recur ctx
                       (concat (or new-txs []) (rest txs))
                       (conj handled tx)))
              (recur ctx (rest txs) handled)))
          handled))))
  ctx)
