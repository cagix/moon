(ns cdq.render.draw-on-world-viewport
  (:require [cdq.ctx.graphics :as graphics]
            [cdq.ctx.graphics]))

(defn do!
  [{:keys [ctx/config
           ctx/graphics]
    :as ctx}]
  (cdq.ctx.graphics/draw-on-world-viewport!
   graphics
   (fn []
     (doseq [f (:draw-on-world-viewport (:cdq.render-pipeline config))]
       (graphics/handle-draws! graphics (f ctx))))))
