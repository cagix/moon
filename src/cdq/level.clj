(ns cdq.level)

(defn create [world-fn ctx]
  ((requiring-resolve world-fn) ctx))
