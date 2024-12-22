(ns ^:no-doc anvil.effect.target.melee-damage
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.world :as world]
            [gdl.utils :refer [defmethods]]))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defmethods :effects.target/melee-damage
  ; FIXME no source
  ; => to entity move
  (component/info [_]
    (str "Damage based on entity strength."
         #_(when source
             (str "\n" (damage-info (entity->melee-damage @source))))))

  (component/applicable? [_ {:keys [effect/source] :as ctx}]
    (component/applicable? (damage-effect @source) ctx))

  (component/handle [_ {:keys [effect/source] :as ctx}]
    (component/handle (damage-effect @source) ctx)))
