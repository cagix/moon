(ns moon.tx
  (:require [moon.component :as component]))

(defn do! [txs]
  (doseq [tx txs]
    (when-let [result (try (cond (not tx) nil
                                 (fn? tx) (tx)
                                 :else (component/handle tx))
                           (catch Throwable t
                             (throw (ex-info "Error with transactions" {:tx tx} t))))]
      (do! result))))
