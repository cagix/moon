(ns cdq.render.draw-on-world-viewport
  (:require [cdq.ctx.graphics :as graphics]
            [cdq.ctx.graphics]))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}
   draw-fns]
  (cdq.ctx.graphics/draw-on-world-viewport!
   graphics
   (fn []
     (doseq [f draw-fns]
       (graphics/handle-draws! graphics (f ctx)))))
  ctx)
