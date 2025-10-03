(ns cdq.ctx.render.draw-on-world-viewport
  (:require [cdq.graphics.draws :as draws]
            [cdq.graphics.draw-on-world-viewport :as draw-on-world-viewport]))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}
   draw-fns]
  (draw-on-world-viewport/do! graphics
                              (fn []
                                (doseq [f (map requiring-resolve draw-fns)]
                                  (draws/handle! graphics (f ctx)))))
  ctx)
