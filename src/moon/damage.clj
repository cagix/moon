(ns moon.damage
  (:require [moon.entity.modifiers :as mods]))

(defn- apply-mods [damage entity modifier-k]
  (update damage
          :damage/min-max
          mods/value
          entity
          modifier-k))

(defn modified
  ([source damage]
   (apply-mods damage source :modifier/damage-deal))

  ([source target damage]
   (apply-mods (modified source damage) target :modifier/damage-receive)))

(defn info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))
