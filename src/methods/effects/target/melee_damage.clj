(ns methods.effects.target.melee-damage
  (:require [methods.effects.target.damage :as damage]
            [moon.entity :as entity]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn applicable? [_ {:keys [effect/source] :as ctx}]
  (damage/applicable? (entity->melee-damage @source) ctx))

(defn handle [_ {:keys [effect/source] :as ctx}]
  (damage/handle (entity->melee-damage @source) ctx))
