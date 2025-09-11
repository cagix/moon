(ns cdq.tx.update-potential-fields
  (:require [cdq.potential-fields.update :as potential-fields.update]))

(defn do!
  [{:keys [ctx/factions-iterations
           ctx/potential-field-cache
           ctx/grid
           ctx/active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations)))
