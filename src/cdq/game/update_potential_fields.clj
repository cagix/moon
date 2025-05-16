(ns cdq.game.update-potential-fields
  (:require [cdq.ctx :as ctx]
            [cdq.world.potential-field :as potential-field]))

(defn do! []
  (doseq [[faction max-iterations] ctx/factions-iterations]
    (potential-field/tick! ctx/potential-field-cache
                           ctx/grid
                           faction
                           ctx/active-entities
                           max-iterations)))
