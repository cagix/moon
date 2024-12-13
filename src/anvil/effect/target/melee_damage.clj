(ns anvil.effect.target.melee-damage
  (:require [anvil.component :refer [info applicable? handle]]
            [anvil.entity.stat :as stat]
            [anvil.world :as world]
            [gdl.utils :refer [defmethods]]))

(defn- entity->melee-damage [entity]
  (let [strength (or (stat/->value entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defmethods :effects.target/melee-damage
  ; FIXME no source
  ; => to entity move
  (info [_]
    (str "Damage based on entity strength."
         #_(when source
             (str "\n" (damage-info (entity->melee-damage @source))))))

  (applicable? [_ {:keys [effect/source] :as ctx}]
    (applicable? (damage-effect @source) ctx))

  (handle [_ {:keys [effect/source] :as ctx}]
    (handle (damage-effect @source) ctx)))
