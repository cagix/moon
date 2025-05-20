(ns cdq.tx.effect
  (:require [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.utils :as utils]))

(defn do! [ctx effect-ctx effects]
  (run! #(ctx/handle-txs! (effect/handle % effect-ctx ctx))
        (effect/filter-applicable? effect-ctx effects)))
