(ns cdq.tx.effect
  (:require [cdq.effect :as effect]
            [cdq.g :as g]
            [cdq.utils :as utils]))

(defn do! [ctx effect-ctx effects]
  (run! #(g/handle-txs! ctx (effect/handle % effect-ctx ctx))
        (effect/filter-applicable? effect-ctx effects)))
