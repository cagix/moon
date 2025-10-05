(ns cdq.ctx.render.draw-on-world-viewport
  (:require [cdq.graphics.draws :as draws]
            [cdq.graphics.world-viewport :as world-viewport]))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}
   draw-fns]
  (world-viewport/draw! graphics
                        (fn []
                          (doseq [f (map requiring-resolve draw-fns)]
                            (draws/handle! graphics (f ctx)))))
  ctx)
