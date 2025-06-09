(ns cdq.ctx.effect-handler
  (:require [cdq.utils :as utils]))

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defn handle-txs!
  [{:keys [ctx/world-event-handlers]
    :as ctx}
   transactions]
  (doseq [transaction transactions
          :when transaction]
    (assert (vector? transaction) (pr-str transaction))
    (try (let [result (do! transaction ctx)]
           (when result
             (let [[world-event-k params] result]
               ((utils/safe-get world-event-handlers world-event-k) ctx params))))
         (catch Throwable t
           (throw (ex-info "" {:transaction transaction} t))))))
