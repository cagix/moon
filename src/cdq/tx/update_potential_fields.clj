(ns cdq.tx.update-potential-fields
  (:require [cdq.potential-fields.update :as potential-fields.update]))

(defn do!
  [{:keys [ctx/world]}]
  (let [{:keys [world/active-entities
                world/factions-iterations
                world/grid
                world/potential-field-cache]} world]
    (doseq [[faction max-iterations] factions-iterations]
      (potential-fields.update/tick! potential-field-cache
                                     grid
                                     faction
                                     active-entities
                                     max-iterations))))
