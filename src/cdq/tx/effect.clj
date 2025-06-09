(ns cdq.tx.effect
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.effect :as effect]
            [cdq.world :as world]))

(defmethod do! :tx/effect [[_ effect-ctx effects] ctx]
  (run! #(world/handle-txs! ctx (effect/handle % effect-ctx ctx))
        (effect/filter-applicable? effect-ctx effects)))
