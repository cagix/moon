(ns forge.effects.target.convert
  (:require [clojure.utils :refer [defmethods]]
            [forge.effect :refer [applicable? handle]]
            [forge.entity.faction :as faction]))

(defmethods :effects.target/convert
  (applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (faction/enemy @source))))

  (handle [_ {:keys [effect/source effect/target]}]
    (swap! target assoc :entity/faction (:entity/faction @source))))
