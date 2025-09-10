(ns cdq.render.update-potential-fields
  (:require [cdq.ctx :as ctx]))

(defn do!
  [ctx]
  (if (:ctx/paused? ctx)
    ctx
    (do
     (ctx/handle-txs! ctx [[:tx/update-potential-fields]])
     ctx)))
