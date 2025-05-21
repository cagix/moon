(ns cdq.config
  (:require [cdq.utils :refer [io-slurp-edn safe-get]]))

(defn create [_ctx path]
  (let [m (io-slurp-edn path)]
    (reify clojure.lang.ILookup
      (valAt [_ k]
        (safe-get m k)))))
