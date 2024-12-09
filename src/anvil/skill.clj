(ns anvil.skill
  (:require [anvil.effect :as effect]
            [anvil.mana :as mana]))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana/val entity))))

(defn usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill} effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect/applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))
