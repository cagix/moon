(ns moon.damage
  (:require [moon.val-max :as val-max]))

(defn modified
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (val-max/apply-min-modifier source :modifier/damage-deal-min)
                (val-max/apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (modified source damage)
           :damage/min-max
           val-max/apply-max-modifier
           target
           :modifier/damage-receive-max)))

(defn info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))
