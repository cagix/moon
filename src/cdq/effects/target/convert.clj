(ns cdq.effects.target.convert
  (:require [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.utils :refer [defmethods]]))

(defmethods :effects.target/convert
  (effect/applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (entity/faction @target)
            (entity/enemy @source))))

  (effect/handle [_ {:keys [effect/source effect/target]} _world]
    [[:tx/assoc target :entity/faction (entity/faction @source)]]))
