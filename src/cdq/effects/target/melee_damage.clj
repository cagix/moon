(ns cdq.effects.target.melee-damage
  (:require [cdq.world.effect :as effect]
            [cdq.world.entity :as entity]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defn applicable? [_ {:keys [effect/source] :as effect-ctx}]
  (effect/applicable? (melee-damage-effect @source) effect-ctx))

(defn handle [_ {:keys [effect/source] :as effect-ctx} world]
  (effect/handle (melee-damage-effect @source) effect-ctx world))
