(ns forge.effects.target.melee-damage
  (:require [anvil.stat :as stat]
            [clojure.component :as component]))

(defn- entity->melee-damage [entity]
  (let [strength (or (stat/->value entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defn applicable? [_ {:keys [effect/source] :as ctx}]
  (component/applicable? (damage-effect @source)
                         ctx))

(defn handle [_ {:keys [effect/source] :as ctx}]
  (component/handle (damage-effect @source)
                    ctx))
