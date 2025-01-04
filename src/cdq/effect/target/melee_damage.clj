(ns cdq.effect.target.melee-damage
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defn applicable? [_ {:keys [effect/source] :as ctx}]
  (effect/applicable? (damage-effect @source) ctx))

(defn handle [_ {:keys [effect/source] :as ctx} c]
  (effect/handle (damage-effect @source) ctx c))
