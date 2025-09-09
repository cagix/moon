(ns cdq.render.draw-on-world-viewport
  (:require [cdq.graphics :as graphics]))

(defn do!
  [{:keys [ctx/config]
    :as ctx}]
  (graphics/draw-on-world-viewport!
   ctx
   (fn []
     (doseq [f (:draw-on-world-viewport (:cdq.render-pipeline config))]
       ((requiring-resolve f) ctx))))
  ctx)
