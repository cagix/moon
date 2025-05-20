#_(ns cdq.config
  (:require [cdq.utils :refer [io-slurp-edn safe-get]]))

#_(defn create [path _ctx]
  (let [m (io-slurp-edn path)]
    (reify ILookup
      (valAt [_ k]
        (safe-get m k)))))
