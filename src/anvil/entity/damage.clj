(ns ^:no-doc anvil.entity.damage
  (:require [anvil.entity :as entity]))

(defn-impl entity/damage
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (entity/apply-min-modifier source :modifier/damage-deal-min)
                (entity/apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (entity/damage source damage)
           :damage/min-max
           entity/apply-max-modifier
           target
           :modifier/damage-receive-max)))
