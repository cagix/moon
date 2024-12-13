(ns anvil.effect.target.convert
  (:require [anvil.component :refer [info applicable? handle]]
            [anvil.entity.faction :as faction]
            [anvil.world :as world]
            [gdl.utils :refer [defmethods]]))

(defmethods :effects.target/convert
  (info [_]
    "Converts target to your side.")

  (applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (faction/enemy @source))))

  (handle [_ {:keys [effect/source effect/target]}]
    (swap! target assoc :entity/faction (:entity/faction @source))))
