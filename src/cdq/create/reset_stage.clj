(ns cdq.create.reset-stage
  (:require [cdq.ctx :as ctx]))

; tx returns nil and not ctx so cant use it direclty
(defn do!
  [ctx]
  (ctx/handle-txs! ctx [[:tx/reset-stage]])
  ctx)
