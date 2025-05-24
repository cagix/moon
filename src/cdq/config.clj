(ns cdq.config
  (:require [cdq.utils]
            [gdl.utils])
  (:import (clojure.lang ILookup)))

(defn create [path]
  (let [m (cdq.utils/io-slurp-edn path)]
    (reify ILookup
      (valAt [_ k]
        (gdl.utils/safe-get m k)))))
