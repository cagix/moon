(ns cdq.render.draw-on-world-viewport
  (:require [cdq.graphics :as graphics]))

(defn do!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (doseq [f '[cdq.draw-on-world-viewport.tile-grid/do!
                                                  cdq.draw-on-world-viewport.cell-debug/do!
                                                  cdq.draw-on-world-viewport.entities/do!
                                                  ;cdq.draw-on-world-viewport.geom-test/do!
                                                  cdq.draw-on-world-viewport.highlight-mouseover-tile/do!]]
                                        ((requiring-resolve f) ctx))))
  ctx)
