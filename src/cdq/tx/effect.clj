(ns cdq.tx.effect
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.effect-handler :refer [do!]]
            [cdq.effect :as effect]))

(defmethod do! :tx/effect [[_ effect-ctx effects] ctx]
  (run! #(ctx/handle-txs! ctx (effect/handle % effect-ctx ctx))
        (effect/filter-applicable? effect-ctx effects)))
