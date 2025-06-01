(ns cdq.render.draw-on-world-viewport
  (:require [cdq.graphics :as g]))

(def draw-fns
  '[
    cdq.render.draw-on-world-viewport.draw-tile-grid/do!
    cdq.render.draw-on-world-viewport.draw-cell-debug/do!
    cdq.render.draw-on-world-viewport.render-entities/do!
    ; cdq.render.draw-on-world-viewport.geom-test/do!
    cdq.render.draw-on-world-viewport.highlight-mouseover-tile/do!
    ])

(defn do! [ctx]
  (let [draw-fns (map requiring-resolve draw-fns)]
    (g/draw-on-world-viewport! ctx
                               (fn []
                                 (doseq [f draw-fns]
                                   (f ctx)))))
  ctx)
