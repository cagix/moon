(ns component.tx
  (:require [component.core :refer [defsystem]]))

(defsystem handle)

(defn do! [txs]
  (doseq [tx txs]
    (when-let [result (try (cond (not tx) nil
                                 (fn? tx) (tx)
                                 :else (handle tx))
                           (catch Throwable t
                             (throw (ex-info "Error with transactions" {:tx tx} t))))]
      (do! result))))
