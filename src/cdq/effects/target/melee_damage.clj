(ns cdq.effects.target.melee-damage
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defcomponent :effects.target/melee-damage
  (effect/applicable? [_ {:keys [effect/source] :as ctx}]
    (effect/applicable? (melee-damage-effect @source) ctx))

  (effect/handle [_ {:keys [effect/source] :as ctx}]
    (effect/handle (melee-damage-effect @source) ctx)))
