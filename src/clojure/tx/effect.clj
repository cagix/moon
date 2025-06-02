(ns clojure.tx.effect
  (:require [clojure.ctx :as ctx]
            [clojure.ctx.effect-handler :refer [do!]]
            [clojure.effect :as effect]))

(defmethod do! :tx/effect [[_ effect-ctx effects] ctx]
  (run! #(ctx/handle-txs! ctx (effect/handle % effect-ctx ctx))
        (effect/filter-applicable? effect-ctx effects)))
