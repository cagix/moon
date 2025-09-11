(ns cdq.world.raycaster
  (:require [cdq.grid.cell :as cell]
            [cdq.grid2d :as g2d]
            [cdq.math.raycaster :as raycaster]))

(defn create [g2d]
  (let [width  (g2d/width  g2d)
        height (g2d/height g2d)
        cells  (for [cell (map deref (g2d/cells g2d))]
                 [(:position cell)
                  (boolean (cell/blocks-vision? cell))])]
    (raycaster/create width height cells)))
