(ns cdq.skill
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (entity/mana-val entity))))

(defn usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill} effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect/some-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))
