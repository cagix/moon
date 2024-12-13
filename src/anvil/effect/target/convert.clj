(ns anvil.effect.target.convert
  (:require [anvil.component :refer [applicable? handle]]
            [anvil.entity.faction :as faction]
            [anvil.world :as world]
            [gdl.utils :refer [defmethods]]))

(defmethods :effects.target/convert
  (applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (faction/enemy @source))))

  (handle [_ {:keys [effect/source effect/target]}]
    (swap! target assoc :entity/faction (:entity/faction @source))))
