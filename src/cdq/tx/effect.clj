(ns cdq.tx.effect
  (:require [cdq.effect :as effect]
            [cdq.utils :as utils]))

(defn do! [effect-ctx effects]
  (run! #(utils/handle-txs! (effect/handle % effect-ctx))
        (effect/filter-applicable? effect-ctx effects)))
