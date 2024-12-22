(ns anvil.skill.usable-state
  (:require [anvil.effect :as effect]
            [anvil.entity.mana :as mana]
            [anvil.skill :as skill]
            [gdl.utils :refer [defn-impl]]))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana/val entity))))

(defn-impl skill/usable-state
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
