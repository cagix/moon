(ns forge.effects.target.melee-damage
  (:require [forge.effect :as effect]
            [forge.entity.stat :as stat]))

(defn- entity->melee-damage [entity]
  (let [strength (or (stat/->value entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defn applicable? [_ {:keys [effect/source] :as ctx}]
  (effect/applicable? (damage-effect @source)
                      ctx))

(defn handle [_ {:keys [effect/source] :as ctx}]
  (effect/handle (damage-effect @source)
                 ctx))
