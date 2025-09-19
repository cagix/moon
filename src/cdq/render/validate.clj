(ns cdq.render.validate
  (:require [cdq.ctx :as ctx]))

(defn do! [ctx]
  (ctx/validate ctx)
  ctx)
