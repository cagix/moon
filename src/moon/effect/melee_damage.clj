(ns moon.effect.melee-damage
  (:require [moon.component :as component]
            [moon.effect :refer [source]]
            [moon.entity :as entity]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :stats/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect []
  [:effect.entity/damage (entity->melee-damage @source)])

(defc :effect.entity/melee-damage
  {:schema :some}
  (component/info [_]
    (str "Damage based on entity strength."
         (when source
           (str "\n" (component/info (damage-effect))))))

  (component/applicable? [_]
    (component/applicable? (damage-effect)))

  (component/handle [_]
    [(damage-effect)]))
