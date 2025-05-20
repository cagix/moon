(ns cdq.tx.effect
  (:require [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.utils :as utils]))

(defn do! [_ctx effect-ctx effects]
  (run! #(ctx/handle-txs! (effect/handle % effect-ctx))
        (effect/filter-applicable? effect-ctx effects)))
