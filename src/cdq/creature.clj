(ns cdq.creature
  (:require [cdq.effect :as effect]
            [cdq.stats :as stats]))

(defn skill-usable-state [entity
                          {:keys [skill/cooling-down? skill/effects] :as skill}
                          effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (stats/not-enough-mana? (:creature/stats entity) skill)
   :not-enough-mana

   (not (seq (filter #(effect/applicable? % effect-ctx) effects)))
   :invalid-params

   :else
   :usable))
