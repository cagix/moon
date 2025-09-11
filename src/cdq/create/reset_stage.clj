(ns cdq.create.reset-stage
  (:require [cdq.ctx :as ctx]))

(defn do!
  [ctx]
  (ctx/handle-txs! ctx [[:tx/reset-stage]])
  ctx)
