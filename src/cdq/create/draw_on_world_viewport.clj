(ns cdq.create.draw-on-world-viewport
  (:require cdq.draw-on-world-viewport.tile-grid
            cdq.draw-on-world-viewport.cell-debug
            cdq.draw-on-world-viewport.entities
            cdq.draw-on-world-viewport.geom-test
            cdq.draw-on-world-viewport.highlight-mouseover-tile))

(defn do! [ctx]
  (assoc ctx :ctx/draw-on-world-viewport [cdq.draw-on-world-viewport.tile-grid/do!
                                          cdq.draw-on-world-viewport.cell-debug/do!
                                          cdq.draw-on-world-viewport.entities/do!
                                          ;cdq.draw-on-world-viewport.geom-test/do! : TODO can this be an independent test ?
                                          cdq.draw-on-world-viewport.highlight-mouseover-tile/do!]))
