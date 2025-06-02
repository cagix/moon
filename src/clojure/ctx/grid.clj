(ns clojure.ctx.grid
  (:require [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.potential-fields.movement :as potential-fields.movement]))

(defn nearest-enemy-distance [{:keys [ctx/grid]} entity]
  (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                (entity/enemy entity)))

(defn nearest-enemy [{:keys [ctx/grid]} entity]
  (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                       (entity/enemy entity)))

(defn potential-field-find-direction [{:keys [ctx/grid]} eid]
  (potential-fields.movement/find-direction grid eid))
