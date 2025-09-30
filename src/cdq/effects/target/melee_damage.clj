(ns cdq.effects.target.melee-damage
  (:require [cdq.effect :as effect]
            [cdq.entity.stats :as stats]))

(defn- entity->melee-damage [{:keys [entity/stats]}]
  (let [strength (or (stats/get-stat-value stats :stats/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defn applicable? [_ {:keys [effect/source] :as effect-ctx}]
  (effect/applicable? (melee-damage-effect @source) effect-ctx))

(defn handle [_ {:keys [effect/source] :as effect-ctx} world]
  (effect/handle (melee-damage-effect @source) effect-ctx world))

(defn info-text [_ _world]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))
