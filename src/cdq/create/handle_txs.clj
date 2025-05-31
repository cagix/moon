(ns cdq.create.handle-txs
  (:require [cdq.g :as g]
            [cdq.g.handle-txs]))

(defn do! [ctx]
  (extend (class ctx)
    g/EffectHandler
    {:handle-txs!
     (fn [ctx transactions]
       (doseq [transaction transactions
               :when transaction
               :let [_ (assert (vector? transaction)
                               (pr-str transaction))
                     ; TODO also should be with namespace 'tx' the first is a keyword
                     ]]
         (try (cdq.g.handle-txs/handle-tx! transaction ctx)
              (catch Throwable t
                (throw (ex-info "" {:transaction transaction} t))))))})
  ctx)
