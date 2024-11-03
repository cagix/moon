(ns moon.effect.entity.melee-damage
  (:require [moon.component :as component]
            [moon.effect :refer [source]]
            [moon.entity :as entity]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :stats/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect []
  [:effect.entity/damage (entity->melee-damage @source)])

(defn info [_]
  (str "Damage based on entity strength."
       (when source
         (str "\n" (component/info (damage-effect))))))

(defn applicable? [_]
  (component/applicable? (damage-effect)))

(defn handle [_]
  [(damage-effect)])
