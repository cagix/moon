(ns anvil.entity.damage
  (:require [anvil.entity.modifiers :as mods]))

(defn ->value
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (mods/apply-min-modifier source :modifier/damage-deal-min)
                (mods/apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (->value source damage)
           :damage/min-max
           mods/apply-max-modifier
           target
           :modifier/damage-receive-max)))
