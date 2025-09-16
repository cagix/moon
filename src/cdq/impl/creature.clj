(ns cdq.impl.creature
  (:require [cdq.creature :as creature]
            [cdq.effect :as effect]
            [cdq.stats :as modifiers]))

(defn skill-usable-state
  [entity
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

(defn init! [ctx]
  (extend-type cdq.tx.spawn_entity.Entity
    creature/Skills
    (skill-usable-state [entity skill effect-ctx]
      (skill-usable-state entity skill effect-ctx)))
  ctx)
