(ns cdq.render.draw-on-world-viewport
  (:require [gdl.graphics :as graphics]))

(def draw-fns
  '[
    cdq.render.draw-on-world-viewport.draw-tile-grid/do!
    cdq.render.draw-on-world-viewport.draw-cell-debug/do!
    cdq.render.draw-on-world-viewport.render-entities/do!
    ; cdq.render.draw-on-world-viewport.geom-test/do!
    cdq.render.draw-on-world-viewport.highlight-mouseover-tile/do!
    ])

(defn do! [{:keys [ctx/graphics]
            :as ctx}]
  (let [draw-fns (map requiring-resolve draw-fns)]
    (graphics/draw-on-world-viewport! graphics
                                      (fn []
                                        (doseq [f draw-fns]
                                          (f ctx)))))
  ctx)
