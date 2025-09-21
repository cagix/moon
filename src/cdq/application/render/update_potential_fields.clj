(ns cdq.application.render.update-potential-fields
  (:require [cdq.ctx :as ctx]))

(defn do!
  [ctx]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (do
     (ctx/handle-txs! ctx [[:tx/update-potential-fields]])
     ctx)))
