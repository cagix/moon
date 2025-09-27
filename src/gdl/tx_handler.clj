(ns gdl.tx-handler)

(defn actions!
  [txs-fn-map ctx txs & {:keys [strict?]}]
  (loop [ctx ctx
         txs txs
         handled-txs []]
    (if (empty? txs)
      handled-txs
      (let [[k & params :as tx] (first txs)]
        (if tx
          (let [_ (assert (vector? tx))
                f (get txs-fn-map k)
                _ (if strict?
                    (assert f (str "Cannot find function for tx: " k))
                    nil)
                new-txs (try
                         (if (and (not strict?)
                                  (nil? f))
                           nil
                           (apply f ctx params))
                         (catch Throwable t
                           (throw (ex-info "Error handling tx"
                                           {:tx tx}
                                           t))))]
            (recur ctx
                   (concat (or new-txs []) (rest txs))
                   (conj handled-txs tx)))
          (recur ctx
                 (rest txs)
                 handled-txs))))))
