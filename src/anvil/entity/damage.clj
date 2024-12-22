(ns ^:no-doc anvil.entity.damage
  (:require [anvil.entity :as entity]
            [anvil.entity.modifiers :as mods]))

(defn-impl entity/damage
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (mods/apply-min-modifier source :modifier/damage-deal-min)
                (mods/apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (entity/damage source damage)
           :damage/min-max
           mods/apply-max-modifier
           target
           :modifier/damage-receive-max)))
