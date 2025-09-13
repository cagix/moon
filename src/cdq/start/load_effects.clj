(ns cdq.start.load-effects
  (:require cdq.effect
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.symbol :as symbol]))

(defn do! [ctx]
  (.bindRoot #'cdq.effect/k->method-map (symbol/require-resolve-symbols
                                         (-> "effects_fn_map.edn"
                                             io/resource
                                             slurp
                                             edn/read-string)))
  ctx)
