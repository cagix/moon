(ns cdq.render.update-potential-fields
  (:require [cdq.potential-fields.update :as potential-fields.update]))

(defn do!
  [{:keys [ctx/grid
           ctx/world]
    :as ctx}]
  (doseq [[faction max-iterations] (:world/factions-iterations  world)]
    (potential-fields.update/tick! (:world/potential-field-cache world)
                                   grid
                                   faction
                                   (:world/active-entities world)
                                   max-iterations))
  ctx)
