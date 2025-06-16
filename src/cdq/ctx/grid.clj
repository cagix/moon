(ns cdq.ctx.grid
  (:require [cdq.cell :as cell]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.potential-fields.movement :as potential-fields.movement]))

(defn nearest-enemy-distance [{:keys [ctx/world]} entity]
  (cell/nearest-entity-distance @(grid/cell (:world/grid world) (mapv int (entity/position entity)))
                                (entity/enemy entity)))

(defn nearest-enemy [{:keys [ctx/world]} entity]
  (cell/nearest-entity @(grid/cell (:world/grid world) (mapv int (entity/position entity)))
                       (entity/enemy entity)))

(defn potential-field-find-direction [{:keys [ctx/world]} eid]
  (potential-fields.movement/find-direction (:world/grid world) eid))
