(ns cdq.skill
  (:require [cdq.effect :as effect]
            [cdq.stats :as modifiers]))

(defn usable-state [entity
                    {:keys [skill/cooling-down? skill/effects] :as skill}
                    effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (modifiers/not-enough-mana? (:creature/stats entity) skill)
   :not-enough-mana

   (not (effect/some-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))
