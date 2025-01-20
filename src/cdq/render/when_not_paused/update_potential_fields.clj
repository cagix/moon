(ns cdq.render.when-not-paused.update-potential-fields
  (:require cdq.potential-fields))

(defn render [{:keys [cdq.context/factions-iterations
                      cdq.context/grid
                      world/potential-field-cache
                      cdq.game/active-entities]
               :as context}]
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.potential-fields/tick potential-field-cache
                               grid
                               faction
                               active-entities
                               max-iterations))
  context)
