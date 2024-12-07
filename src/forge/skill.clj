(ns forge.skill
  (:require [forge.effect :refer [effects-applicable?]]
            [forge.entity.mana :refer [mana-value]]))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-value entity))))

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

