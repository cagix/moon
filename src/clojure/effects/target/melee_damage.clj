(ns clojure.effects.target.melee-damage
  (:require [clojure.effect :as effect]
            [clojure.entity :as entity]
            [clojure.utils :refer [defcomponent]]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defcomponent :effects.target/melee-damage
  (effect/applicable? [_ {:keys [effect/source] :as effect-ctx}]
    (effect/applicable? (melee-damage-effect @source) effect-ctx))

  (effect/handle [_ {:keys [effect/source] :as effect-ctx} ctx]
    (effect/handle (melee-damage-effect @source) effect-ctx ctx)))
