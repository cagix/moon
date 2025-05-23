(ns cdq.application.config
  (:require [cdq.utils :refer [io-slurp-edn]]
            [gdl.utils :refer [safe-get]])
  (:import (clojure.lang ILookup)))

(defn create [path]
  (let [m (io-slurp-edn path)]
    (reify ILookup
      (valAt [_ k]
        (safe-get m k)))))
