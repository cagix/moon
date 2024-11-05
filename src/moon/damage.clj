(ns moon.damage
  (:require [malli.core :as m]
            [moon.entity.modifiers :as mods]
            [moon.val-max :as val-max]))

(defn- apply-mods [damage entity modifier-k min-max-idx]
  {:pre [(#{0 1} min-max-idx)
         (= "modifier" (namespace modifier-k))]}
  (update-in damage
             [:damage/min-max min-max-idx]
             mods/value
             entity
             modifier-k))

(defn- ->pos-int [v]
  (-> v int (max 0)))

(defn- align-min-max [min-max]
  {:post [(m/validate val-max/schema %)]}
  (let [[mn mx] (mapv ->pos-int min-max)]
    [mn (max mn mx)]))

(defn- align [damage]
  (update damage :damage/min-max align-min-max))

(defn modified
  ([source damage]
   (-> damage
       (apply-mods source :modifier/damage-deal-min 0)
       (apply-mods source :modifier/damage-deal-max 1)
       align))

  ([source target damage]
   (-> (modified source damage)
       (apply-mods target :modifier/damage-receive-max 1)
       align)))

(defn info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))
