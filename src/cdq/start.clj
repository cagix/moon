(ns cdq.start
  (:require cdq.core))

(defn -main []
  (cdq.core/load! "effects_impl.edn")
  (cdq.core/load! "txs_impl.edn")
  (cdq.core/load! "cdq.app.edn"))
