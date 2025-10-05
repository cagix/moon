(ns cdq.ctx.render.draw-on-world-viewport
  (:require [cdq.graphics :as graphics]
            [cdq.graphics.draws :as draws]))

(defn do!
  [{:keys [ctx/graphics]
    :as ctx}
   draw-fns]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (doseq [f (map requiring-resolve draw-fns)]
                                        (draws/handle! graphics (f ctx)))))
  ctx)
