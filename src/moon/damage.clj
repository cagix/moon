(ns moon.damage
  (:require [malli.core :as m]
            [moon.entity.modifiers :as mods]
            [moon.val-max :as val-max]))

(defn- ->pos-int [v]
  (-> v int (max 0)))

(defn- apply-mods [min-max entity modifier-k min-max-idx]
  {:pre [(m/validate val-max/schema min-max)
         (#{0 1} min-max-idx)
         (= "modifier" (namespace modifier-k))]
   :post [(m/validate val-max/schema min-max)]}
  (let [min-max (update min-max min-max-idx mods/value entity modifier-k)
        [v mx] (mapv ->pos-int min-max) ]
    (case min-max-idx
      0 [v (max v mx)]
      1 [(min v mx) mx])))

(defn modified
  ([source damage]
   (update damage :damage/min-max #(-> %
                                       (apply-mods source :modifier/damage-deal-min 0)
                                       (apply-mods source :modifier/damage-deal-max 1))))

  ([source target damage]
   (update (modified source damage)
           :damage/min-max
           apply-mods
           target
           :modifier/damage-receive-max 1)))

(defn info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))
