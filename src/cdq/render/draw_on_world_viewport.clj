(ns cdq.render.draw-on-world-viewport
  (:require [cdq.graphics :as graphics]))

(defn do! [{:keys [ctx/graphics] :as ctx} draw-fns]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (doseq [f draw-fns]
                                        (f ctx))))
  ctx)
