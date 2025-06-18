(ns cdq.render.update-potential-fields
  (:require [cdq.potential-fields.update :as potential-fields.update]))

(defn- tick-potential-fields!
  [{:keys [world/factions-iterations
           world/potential-field-cache
           world/grid
           world/active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations)))

(defn do!
  [{:keys [ctx/world]
    :as ctx}]
  (tick-potential-fields! world)
  ctx)
