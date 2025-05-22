(ns cdq.g.potential-field
  (:require [cdq.g :as g]
            [cdq.potential-field.movement :as potential-field]))

(extend-type cdq.g.Game
  g/PotentialField
  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-field/find-direction grid eid)))
