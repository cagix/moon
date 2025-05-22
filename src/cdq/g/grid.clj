(ns cdq.g.grid
  (:require [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.g :as g]))

(extend-type cdq.g.Game
  cdq.g/Grid
  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid (mapv int (:position entity)))
                                  (entity/enemy entity)))
  )
