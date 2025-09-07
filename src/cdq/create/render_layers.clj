(ns cdq.create.render-layers
  (:require cdq.entity.render))

(defn do! [ctx]
  (assoc ctx :ctx/render-layers cdq.entity.render/render-layers))
