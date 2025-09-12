(ns cdq.render.draw-on-world-viewport
  (:require [cdq.ctx.graphics :as graphics]
            [cdq.gdx.graphics]))

(defn do!
  [{:keys [ctx/config
           ctx/graphics]
    :as ctx}]
  (cdq.gdx.graphics/draw-on-world-viewport!
   graphics
   (fn []
     (doseq [f (:draw-on-world-viewport (:cdq.render-pipeline config))]
       (graphics/handle-draws! graphics ((requiring-resolve f) ctx))))))
