(ns cdq.game.update-potential-fields
  (:require [cdq.ctx :as ctx]
            [cdq.world.potential-fields :as potential-fields]))

(defn do! []
  (doseq [[faction max-iterations] {:good 15 :evil 5}]
    (potential-fields/tick (:potential-field-cache ctx/world)
                           (:grid ctx/world)
                           faction
                           (:active-entities ctx/world)
                           max-iterations)))
