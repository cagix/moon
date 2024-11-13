(ns moon.effects.entity.melee-damage
  (:require [moon.effects.entity.damage :as damage]
            [moon.entity.stat :as stat]))

(defn- entity->melee-damage [entity]
  (let [strength (or (stat/value entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

; FIXME no source
(defn info [_]
  (str "Damage based on entity strength."
       #_(when source
         (str "\n" (damage/info (entity->melee-damage @source))))))

(defn applicable? [_ {:keys [effect/source] :as ctx}]
  (damage/applicable? (entity->melee-damage @source) ctx))

(defn handle [_ {:keys [effect/source] :as ctx}]
  (damage/handle (entity->melee-damage @source) ctx))
