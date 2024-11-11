(ns moon.effect.entity.melee-damage
  (:require [moon.effects :refer [source]]
            [moon.effect.entity.damage :as damage]
            [moon.entity.stat :as stat]))

(defn- entity->melee-damage [entity]
  (let [strength (or (stat/value entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn info [_]
  (str "Damage based on entity strength."
       (when source
         (str "\n" (damage/info (entity->melee-damage @source))))))

(defn applicable? [_]
  (damage/applicable? (entity->melee-damage @source)))

(defn handle [_]
  (damage/handle (entity->melee-damage @source)))
