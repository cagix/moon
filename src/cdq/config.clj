(ns cdq.config
  (:require [cdq.utils]
            [gdl.utils])
  (:import (clojure.lang ILookup)))

(def log? false)

(defn create [path]
  (let [m (cdq.utils/require-symbols (cdq.utils/io-slurp-edn path)
                                     log?)]
    (reify ILookup
      (valAt [_ k]
        (gdl.utils/safe-get m k)))))
