(ns cdq.render.draw-on-world-viewport
  (:require [cdq.graphics :as graphics]
            [cdq.gdx.graphics]))

(defn do!
  [{:keys [ctx/config]
    :as ctx}]
  (cdq.gdx.graphics/draw-on-world-viewport!
   ctx
   (fn []
     (doseq [f (:draw-on-world-viewport (:cdq.render-pipeline config))]
       (graphics/handle-draws! ctx ((requiring-resolve f) ctx))))))
