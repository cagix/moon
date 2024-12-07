(ns forge.effects.target.melee-damage
  (:require [clojure.utils :refer [defmethods]]
            [forge.effect :refer [applicable? handle]]
            [forge.entity.stat :as stat]))

(defn- entity->melee-damage [entity]
  (let [strength (or (stat/->value entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defmethods :effects.target/melee-damage
  (applicable? [_ {:keys [effect/source] :as ctx}]
    (applicable? [:effects.target/damage (entity->melee-damage @source)] ctx))

  (handle [_ {:keys [effect/source] :as ctx}]
    (handle [:effects.target/damage (entity->melee-damage @source)] ctx)))
