(ns cdq.render.update-potential-fields
  (:require [cdq.world :as world]))

(defn do! [ctx]
  (world/update-potential-fields! ctx)
  ctx)
