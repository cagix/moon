(ns ^:no-doc moon.damage
  (:require [moon.entity :as entity]))

(defn modified
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (entity/apply-min-modifier source :modifier/damage-deal-min)
                (entity/apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (modified source damage)
           :damage/min-max
           entity/apply-max-modifier
           target
           :modifier/damage-receive-max)))

(defn info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))
