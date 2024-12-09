(ns forge.skill
  (:require [anvil.entity :as entity]
            [forge.effect :refer [effects-applicable?]]))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (entity/mana-value entity))))

(defn usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill} effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effects-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

